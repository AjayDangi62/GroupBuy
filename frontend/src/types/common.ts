export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface ApiError {
  timestamp?: string;
  status?: number;
  errorCode?: string;
  message: string;
  fieldErrors?: {
    field: string;
    rejectedValue?: unknown;
    message: string;
  }[];
}

