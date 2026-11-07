"""Executable standard-library smoke tests for the local receiver."""
import http.client
import json
import os
import pathlib
import subprocess
import sys
import tempfile
import time
import unittest
import uuid
from urllib.error import URLError
from urllib.request import Request, urlopen
from sixpay_callback_mock import signature

class ReceiverTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        cls.port = 18765
        cls.secret = 'test-only-local-secret-2026'
        env = os.environ.copy()
        env.update(SIXPAY_MOCK_HMAC_SECRET=cls.secret, SIXPAY_MOCK_ADMIN_TOKEN='test-admin-token')
        cls.proc = subprocess.Popen([sys.executable, str(pathlib.Path(__file__).with_name('sixpay_callback_mock.py')), '--port', str(cls.port), '--db', str(pathlib.Path(cls.temp.name)/'receiver.sqlite3')], env=env, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
        for _ in range(50):
            try:
                cls.admin('GET', '/__mock/diagnostics')
                break
            except (URLError, ConnectionRefusedError):
                time.sleep(0.1)
        else:
            raise RuntimeError('receiver did not start')

    @classmethod
    def tearDownClass(cls):
        cls.proc.terminate()
        cls.proc.wait(timeout=5)
        cls.proc.stderr.close()
        cls.temp.cleanup()

    @classmethod
    def admin(cls, method, path, payload=None):
        data = None if payload is None else json.dumps(payload).encode()
        with urlopen(Request(f'http://127.0.0.1:{cls.port}{path}', data=data, method=method, headers={'X-Mock-Admin-Token':'test-admin-token', 'Content-Type':'application/json'}), timeout=3) as response:
            return json.loads(response.read())

    @classmethod
    def send(cls, event, delivery=None, attempt=1, bad_sig=False):
        data=json.dumps(event, separators=(',', ':')).encode()
        ts=str(int(time.time()))
        sig=signature(cls.secret,ts,'POST','/sixpay/callback',data)
        if bad_sig: sig='v1=INVALID'
        c=http.client.HTTPConnection('127.0.0.1',cls.port,timeout=3)
        c.request('POST','/sixpay/callback',data,{'Content-Type':'application/json','X-Correlation-ID':event['correlationId'],'X-Webhook-Event-ID':event['eventId'],'X-Webhook-Delivery-ID':delivery or str(uuid.uuid4()),'X-Webhook-Delivery-Attempt':str(attempt),'X-SIXPAY-Signature':sig,'X-SIXPAY-Signature-Key-ID':'local-test-v1','X-SIXPAY-Signature-Timestamp':ts})
        resp=c.getresponse(); status=resp.status;resp.read();c.close();return status

    def setUp(self):
        self.admin('PUT', '/__mock/mode', {'mode':'normal'})
        self.event=dict(schemaVersion='1.0',eventId=str(uuid.uuid4()),eventType='CUT_CREDITED',occurredAt='2026-11-07T12:00:00Z',correlationId=str(uuid.uuid4()),paymentId=str(uuid.uuid4()),paymentReference='PAY-TEST-001',tresorPayPaymentReference='REF-TEST-001',financialInstitutionCode='TEST',paymentVersion=1,data={'bankPostingReference':'123/456','creditedAt':'2026-11-07T12:00:00Z','immediateResult':'POSTED_PENDING_TFJ','tfjFinality':False,'amount':{'amount':'1.00','currency':'XAF'}})

    def test_accept_duplicate_and_conflict(self):
        self.assertEqual(self.send(self.event),204)
        self.assertEqual(self.send(self.event,attempt=2),204)
        changed=dict(self.event);changed['paymentVersion']=2
        self.assertEqual(self.send(changed,attempt=3),409)

    def test_invalid_signature(self):
        self.assertEqual(self.send(self.event,bad_sig=True),401)

    def test_503_without_persistence(self):
        self.admin('PUT','/__mock/mode',{'mode':'503'})
        self.assertEqual(self.send(self.event),503)
        self.admin('PUT','/__mock/mode',{'mode':'normal'})
        self.assertEqual(self.send(self.event,attempt=2),204)

    def test_diagnostics_requires_token(self):
        with self.assertRaises(Exception):
            with urlopen(f'http://127.0.0.1:{self.port}/__mock/diagnostics',timeout=2):pass

if __name__=='__main__': unittest.main(verbosity=2)
