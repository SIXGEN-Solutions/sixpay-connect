import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class RequestIdService {
  generate(): string {
    return globalThis.crypto.randomUUID();
  }
}
