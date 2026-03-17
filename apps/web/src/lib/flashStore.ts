import { cookies } from 'next/headers';
import { randomUUID } from 'crypto';

type FlashMessage = {
  type: 'success' | 'error';
  message: string;
  createdAt: number;
};

type FormState = {
  data: Record<string, string>;
  createdAt: number;
};

type FlashStore = {
  messages: Map<string, FlashMessage>;
  forms: Map<string, FormState>;
};

const STORE_KEY = 'flash_session_id';
const TTL_MS = 60_000;

function getStore(): FlashStore {
  const globalWithStore = globalThis as typeof globalThis & { __flashStore?: FlashStore };
  if (!globalWithStore.__flashStore) {
    globalWithStore.__flashStore = {
      messages: new Map(),
      forms: new Map()
    };
  }
  return globalWithStore.__flashStore;
}

async function getOrCreateSessionId() {
  const cookieStore = await cookies();
  const existing = cookieStore.get(STORE_KEY)?.value;
  if (existing) {
    return existing;
  }
  const id = randomUUID();
  cookieStore.set(STORE_KEY, id, {
    path: '/',
    httpOnly: true,
    sameSite: 'lax',
    secure: process.env.NODE_ENV === 'production',
    maxAge: Math.floor(TTL_MS / 1000)
  });
  return id;
}

export async function setFlashMessage(type: 'success' | 'error', message: string) {
  const store = getStore();
  const sessionId = await getOrCreateSessionId();
  store.messages.set(sessionId, { type, message, createdAt: Date.now() });
}

export async function consumeFlashMessage(): Promise<FlashMessage | null> {
  const cookieStore = await cookies();
  const sessionId = cookieStore.get(STORE_KEY)?.value;
  if (!sessionId) {
    return null;
  }
  const store = getStore();
  const message = store.messages.get(sessionId);
  if (!message) {
    return null;
  }
  if (Date.now() - message.createdAt > TTL_MS) {
    store.messages.delete(sessionId);
    return null;
  }
  store.messages.delete(sessionId);
  return message;
}

export async function clearFlashMessage() {
  const cookieStore = await cookies();
  const sessionId = cookieStore.get(STORE_KEY)?.value;
  if (!sessionId) {
    return;
  }
  const store = getStore();
  store.messages.delete(sessionId);
}

export async function setFormState(data: Record<string, string>) {
  const store = getStore();
  const sessionId = await getOrCreateSessionId();
  store.forms.set(sessionId, { data, createdAt: Date.now() });
}

export async function consumeFormState(): Promise<Record<string, string> | null> {
  const cookieStore = await cookies();
  const sessionId = cookieStore.get(STORE_KEY)?.value;
  if (!sessionId) {
    return null;
  }
  const store = getStore();
  const formState = store.forms.get(sessionId);
  if (!formState) {
    return null;
  }
  if (Date.now() - formState.createdAt > TTL_MS) {
    store.forms.delete(sessionId);
    return null;
  }
  store.forms.delete(sessionId);
  return formState.data;
}

export async function clearFormState() {
  const cookieStore = await cookies();
  const sessionId = cookieStore.get(STORE_KEY)?.value;
  if (!sessionId) {
    return;
  }
  const store = getStore();
  store.forms.delete(sessionId);
}
