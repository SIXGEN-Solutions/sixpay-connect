export type ApplicationErrorKind =
  | 'network'
  | 'validation'
  | 'unauthorized'
  | 'forbidden'
  | 'not-found'
  | 'rate-limit'
  | 'server'
  | 'generic';

export interface ProblemDetail {
  readonly type: string;
  readonly title: string;
  readonly status: number;
  readonly detail: string;
  readonly instance: string;
  readonly errors?: Readonly<Record<string, string>>;
}

export interface ApplicationError {
  readonly kind: ApplicationErrorKind;
  readonly status: number;
  readonly title: string;
  readonly detail: string;
  readonly fieldErrors: Readonly<Record<string, string>>;
  readonly correlationId: string | null;
  readonly retryAfterSeconds: number | null;
}

export function isProblemDetail(value: unknown): value is ProblemDetail {
  if (!value || typeof value !== 'object') {
    return false;
  }

  const candidate = value as Partial<ProblemDetail>;

  return (
    typeof candidate.type === 'string' &&
    typeof candidate.title === 'string' &&
    typeof candidate.status === 'number' &&
    typeof candidate.detail === 'string' &&
    typeof candidate.instance === 'string'
  );
}
