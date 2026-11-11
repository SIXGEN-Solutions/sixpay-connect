import type { AppEnvironment } from './environment.model';

let activeEnvironment: Readonly<AppEnvironment> | undefined;

export function hasRuntimeEnvironment(): boolean {
  return activeEnvironment !== undefined;
}

export function setRuntimeEnvironment(environment: AppEnvironment): void {
  if (activeEnvironment) throw new Error('SIXPAY runtime configuration already initialized');
  activeEnvironment = Object.freeze(environment);
}

export function getRuntimeEnvironment(): Readonly<AppEnvironment> {
  if (!activeEnvironment) throw new Error('SIXPAY runtime configuration not initialized');
  return activeEnvironment;
}
