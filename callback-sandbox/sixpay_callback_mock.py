#!/usr/bin/env python3
"""SIXPAY callback test receiver (stdlib only). Local development only.

Verifies the exact SHA-256/HMAC signature implemented by
PaymentCallbackHmacSigner at cd0e01d75a3aae3ed8e6cf5dbbf125def52955ac.
The HMAC-only local test profile is NOT a substitute for contract Bearer/API-key auth.
"""
import argparse
import base64
import hashlib
import hmac
import json
import os
import re
import secrets
import sqlite3
import sys
import threading
import time
import uuid
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlsplit

MAX_BYTES = 256 * 1024
MAX_SKEW = 300
UUID_HEADERS = {'X-Correlation-ID', 'X-Webhook-Event-ID', 'X-Webhook-Delivery-ID'}
MODES = {'normal', '503', 'timeout-after-commit', 'timeout-before-commit', 'disconnect'}

def b64url(data):
    return base64.urlsafe_b64encode(data).decode('ascii').rstrip('=')

def signature(secret, timestamp, method, target, raw_body):
    body_hash = b64url(hashlib.sha256(raw_body).digest())
    signing_input = f'{timestamp}.{method.upper()}.{target}.{body_hash}'.encode('utf-8')
    return 'v1=' + b64url(hmac.new(secret.encode('utf-8'), signing_input, hashlib.sha256).digest())

def init_db(path):
    with sqlite3.connect(path, timeout=15) as db:
        db.execute('PRAGMA journal_mode=WAL')
        db.execute('CREATE TABLE IF NOT EXISTS events (event_id TEXT PRIMARY KEY, canonical_sha256 TEXT NOT NULL, raw_sha256 TEXT NOT NULL, event_type TEXT NOT NULL, payment_reference TEXT NOT NULL, first_received_utc TEXT NOT NULL, raw_body BLOB NOT NULL)')
        db.execute('CREATE TABLE IF NOT EXISTS attempts (id INTEGER PRIMARY KEY AUTOINCREMENT, received_utc TEXT NOT NULL, event_id TEXT, delivery_id TEXT, delivery_attempt INTEGER, correlation_id TEXT, outcome TEXT NOT NULL, http_status INTEGER, raw_sha256 TEXT)')
        db.commit()

def iso_now():
    return datetime.now(timezone.utc).isoformat(timespec='seconds')

class Server(ThreadingHTTPServer):
    daemon_threads = True
    allow_reuse_address = True
    def __init__(self, addr, handler, args):
        super().__init__(addr, handler)
        self.args = args
        self.mode = args.mode
        self.lock = threading.Lock()

class Handler(BaseHTTPRequestHandler):
    server_version = 'SIXPAY-Callback-Mock/1.0'
    def log_message(self, fmt, *args):
        # Do not log incoming headers or payloads / credentials.
        print(f'[{iso_now()}] {self.client_address[0]} {fmt % args}', flush=True)

    def reply(self, status, result=None, extra_headers=None):
        body = b'' if result is None else json.dumps(result, separators=(',', ':')).encode()
        self.send_response(status)
        self.send_header('Content-Length', str(len(body)))
        self.send_header('Cache-Control', 'no-store')
        if body:
            self.send_header('Content-Type', 'application/json')
        if extra_headers:
            for k, v in extra_headers.items():
                self.send_header(k, v)
        self.end_headers()
        if body:
            self.wfile.write(body)

    def diagnostic_authorized(self):
        token = self.headers.get('X-Mock-Admin-Token', '')
        return bool(token) and secrets.compare_digest(token, self.server.args.admin_token)

    def do_GET(self):
        if urlsplit(self.path).path != '/__mock/diagnostics':
            return self.reply(404)
        if not self.diagnostic_authorized():
            return self.reply(403)
        with sqlite3.connect(self.server.args.db) as db:
            counts = dict(db.execute('SELECT outcome, COUNT(*) FROM attempts GROUP BY outcome').fetchall())
            events = [dict(eventId=r[0], eventType=r[1], paymentReference=r[2], firstReceivedAt=r[3], rawSha256=r[4]) for r in db.execute('SELECT event_id,event_type,payment_reference,first_received_utc,raw_sha256 FROM events ORDER BY first_received_utc DESC LIMIT 30')]
            attempts = [dict(receivedAt=r[0], eventId=r[1], deliveryId=r[2], deliveryAttempt=r[3], outcome=r[4], httpStatus=r[5]) for r in db.execute('SELECT received_utc,event_id,delivery_id,delivery_attempt,outcome,http_status FROM attempts ORDER BY id DESC LIMIT 50')]
        return self.reply(200, dict(mode=self.server.mode, uniqueEvents=len(events), outcomeCounts=counts, recentEvents=events, recentAttempts=attempts))

    def do_PUT(self):
        if urlsplit(self.path).path != '/__mock/mode':
            return self.reply(404)
        if not self.diagnostic_authorized():
            return self.reply(403)
        try:
            length = int(self.headers.get('Content-Length', '0'))
            if length < 1 or length > 1024:
                return self.reply(400)
            data = json.loads(self.rfile.read(length))
            mode = data['mode']
            if mode not in MODES:
                return self.reply(400, {'allowedModes': sorted(MODES)})
        except (ValueError, KeyError, TypeError, json.JSONDecodeError):
            return self.reply(400)
        with self.server.lock:
            self.server.mode = mode
        print(f'[{iso_now()}] TEST MODE -> {mode}', flush=True)
        return self.reply(200, {'mode': mode})

    def log_attempt(self, event_id, delivery_id, attempt, correlation, outcome, status, raw_hash):
        with sqlite3.connect(self.server.args.db, timeout=15) as db:
            db.execute('INSERT INTO attempts(received_utc,event_id,delivery_id,delivery_attempt,correlation_id,outcome,http_status,raw_sha256) VALUES (?,?,?,?,?,?,?,?)', (iso_now(), event_id, delivery_id, attempt, correlation, outcome, status, raw_hash))
            db.commit()
        print(f'[{iso_now()}] {outcome:<24} status={status} event={event_id} delivery={delivery_id} attempt={attempt}', flush=True)

    def do_POST(self):
        path = urlsplit(self.path).path
        if path != '/sixpay/callback':
            return self.reply(404)
        raw = None
        event_id = delivery_id = corr = None
        attempt = None
        raw_hash = None
        def reject(status, reason):
            self.log_attempt(event_id, delivery_id, attempt, corr, reason, status, raw_hash)
            self.reply(status, {'error': reason})
        try:
            length = int(self.headers.get('Content-Length', '-1'))
            if length < 1 or length > MAX_BYTES:
                return reject(413, 'invalid_body_length')
            raw = self.rfile.read(length)
            if len(raw) != length:
                return reject(400, 'incomplete_body')
            raw_hash = hashlib.sha256(raw).hexdigest()
            for header in UUID_HEADERS:
                value = self.headers.get(header, '')
                try:
                    uuid.UUID(value)
                except ValueError:
                    return reject(400, 'invalid_' + header)
            event_id = str(uuid.UUID(self.headers['X-Webhook-Event-ID']))
            delivery_id = str(uuid.UUID(self.headers['X-Webhook-Delivery-ID']))
            corr = str(uuid.UUID(self.headers['X-Correlation-ID']))
            a = self.headers.get('X-Webhook-Delivery-Attempt', '')
            if not re.fullmatch(r'[1-9][0-9]*', a) or not 1 <= int(a) <= 12:
                return reject(400, 'invalid_delivery_attempt')
            attempt = int(a)
            ts = self.headers.get('X-SIXPAY-Signature-Timestamp', '')
            if not re.fullmatch(r'[0-9]{10}', ts) or abs(time.time() - int(ts)) > MAX_SKEW:
                return reject(401, 'stale_or_invalid_timestamp')
            key_id = self.headers.get('X-SIXPAY-Signature-Key-ID', '')
            if key_id != self.server.args.key_id:
                return reject(401, 'unknown_key_id')
            sent_sig = self.headers.get('X-SIXPAY-Signature', '')
            expected = signature(self.server.args.secret, ts, 'POST', self.path, raw)
            if not hmac.compare_digest(sent_sig, expected):
                return reject(401, 'invalid_hmac')
            if self.server.args.require_auth:
                auth = self.headers.get('Authorization', '')
                key = self.headers.get('X-Subscription-Key', '')
                if not self.server.args.bearer_token or not hmac.compare_digest(auth, 'Bearer ' + self.server.args.bearer_token):
                    return reject(401, 'invalid_bearer')
                if not self.server.args.subscription_key or not hmac.compare_digest(key, self.server.args.subscription_key):
                    return reject(403, 'invalid_subscription_key')
            try:
                document = json.loads(raw)
                if not isinstance(document, dict):
                    return reject(422, 'payload_not_object')
            except (ValueError, UnicodeDecodeError):
                return reject(400, 'invalid_json')
            for field, expected_value in [('eventId', event_id), ('correlationId', corr)]:
                if document.get(field) != expected_value:
                    return reject(422, 'header_body_mismatch_' + field)
            event_type = document.get('eventType')
            if event_type not in {'CUT_CREDITED', 'PAYMENT_REJECTED', 'TREASURY_INTEGRATED'} or document.get('schemaVersion') != '1.0':
                return reject(422, 'unexpected_event_type_or_version')
            if not isinstance(document.get('paymentReference'), str) or not document['paymentReference']:
                return reject(422, 'missing_payment_reference')
            if not isinstance(document.get('paymentVersion'), int) or isinstance(document.get('paymentVersion'), bool) or document['paymentVersion'] < 1:
                return reject(422, 'invalid_payment_version')
            data = document.get('data')
            if not isinstance(data, dict):
                return reject(422, 'missing_event_data')
            if event_type == 'CUT_CREDITED' and (data.get('immediateResult') != 'POSTED_PENDING_TFJ' or data.get('tfjFinality') is not False or not data.get('bankPostingReference')):
                return reject(422, 'invalid_cut_credited_data')
            if event_type == 'PAYMENT_REJECTED' and (data.get('immediateResult') != 'REJECTED' or not isinstance(data.get('reasonCode'), str) or not re.fullmatch(r'[A-Z][A-Z0-9_]{2,63}', data['reasonCode']) or set(data) != {'immediateResult', 'reasonCode'}):
                return reject(422, 'invalid_payment_rejected_data')
            if event_type == 'TREASURY_INTEGRATED' and (not data.get('tfjConfirmationId') or not data.get('bankPostingReference')):
                return reject(422, 'invalid_treasury_data')
            canonical = json.dumps(document, sort_keys=True, separators=(',', ':'), ensure_ascii=False).encode('utf-8')
            canonical_hash = hashlib.sha256(canonical).hexdigest()
            mode = self.server.mode
            if mode == '503':
                self.log_attempt(event_id, delivery_id, attempt, corr, 'simulated_503', 503, raw_hash)
                return self.reply(503, {'error': 'simulated_unavailable'}, {'Retry-After': '2'})
            if mode == 'timeout-before-commit':
                self.log_attempt(event_id, delivery_id, attempt, corr, 'simulated_timeout_before_commit', None, raw_hash)
                time.sleep(self.server.args.timeout_seconds)
                return self.reply(504)
            if mode == 'disconnect':
                self.log_attempt(event_id, delivery_id, attempt, corr, 'simulated_disconnect', None, raw_hash)
                self.close_connection = True
                try:
                    self.connection.shutdown(2)
                except OSError:
                    pass
                self.connection.close()
                return
            with sqlite3.connect(self.server.args.db, timeout=15, isolation_level=None) as db:
                db.execute('BEGIN IMMEDIATE')
                existing = db.execute('SELECT canonical_sha256 FROM events WHERE event_id=?', (event_id,)).fetchone()
                if existing and existing[0] != canonical_hash:
                    db.execute('COMMIT')
                    return reject(409, 'event_id_payload_conflict')
                outcome = 'duplicate_accepted' if existing else 'accepted'
                if not existing:
                    db.execute('INSERT INTO events VALUES (?,?,?,?,?,?,?)', (event_id, canonical_hash, raw_hash, event_type, document['paymentReference'], iso_now(), raw))
                db.execute('INSERT INTO attempts(received_utc,event_id,delivery_id,delivery_attempt,correlation_id,outcome,http_status,raw_sha256) VALUES (?,?,?,?,?,?,?,?)', (iso_now(), event_id, delivery_id, attempt, corr, outcome if mode == 'normal' else 'accepted_then_timeout', 204 if mode == 'normal' else None, raw_hash))
                db.execute('COMMIT')
            print(f'[{iso_now()}] {outcome:<24} eventType={event_type} event={event_id} delivery={delivery_id} attempt={attempt} payment={document["paymentReference"]}', flush=True)
            if mode == 'timeout-after-commit':
                time.sleep(self.server.args.timeout_seconds)
                return self.reply(504)  # client should already have timed out
            return self.reply(204, extra_headers={'X-Correlation-ID': corr})
        except (sqlite3.Error, OSError) as exc:
            print(f'[{iso_now()}] receiver error: {type(exc).__name__}', file=sys.stderr, flush=True)
            try:
                self.reply(500, {'error': 'receiver_storage_failure'})
            except OSError:
                pass


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--host', default='127.0.0.1')
    parser.add_argument('--port', type=int, default=8765)
    parser.add_argument('--db', default='sixpay_callback_mock.sqlite3')
    parser.add_argument('--mode', choices=sorted(MODES), default='normal')
    parser.add_argument('--timeout-seconds', type=int, default=35)
    parser.add_argument('--key-id', default=os.environ.get('SIXPAY_MOCK_KEY_ID', 'local-test-v1'))
    parser.add_argument('--secret', default=os.environ.get('SIXPAY_MOCK_HMAC_SECRET'))
    parser.add_argument('--admin-token', default=os.environ.get('SIXPAY_MOCK_ADMIN_TOKEN'))
    parser.add_argument('--require-auth', action='store_true', help='Require Bearer and subscription key too (from environment)')
    args = parser.parse_args()
    args.bearer_token = os.environ.get('SIXPAY_MOCK_BEARER_TOKEN')
    args.subscription_key = os.environ.get('SIXPAY_MOCK_SUBSCRIPTION_KEY')
    if not args.secret or not args.admin_token:
        parser.error('Set SIXPAY_MOCK_HMAC_SECRET and SIXPAY_MOCK_ADMIN_TOKEN in the local environment')
    if args.require_auth and (not args.bearer_token or not args.subscription_key):
        parser.error('--require-auth requires SIXPAY_MOCK_BEARER_TOKEN and SIXPAY_MOCK_SUBSCRIPTION_KEY')
    if args.host not in {'127.0.0.1', '::1'}:
        parser.error('This development receiver only binds loopback')
    init_db(args.db)
    server = Server((args.host, args.port), Handler, args)
    print(f'[{iso_now()}] SIXPAY mock http://{args.host}:{args.port}/sixpay/callback | db={args.db} | mode={args.mode}', flush=True)
    print('WARNING: local HMAC test mode; Bearer/subscription-key validation is optional and NOT equivalent to JWT verification.', flush=True)
    try:
        server.serve_forever(poll_interval=0.25)
    except KeyboardInterrupt:
        print('\nStopping.', flush=True)
    finally:
        server.server_close()

if __name__ == '__main__':
    main()
