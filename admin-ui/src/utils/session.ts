interface StoredSession { token?: string; scopeId?: string; allUnits?: boolean }
const KEY = 'cyys.session';
export const CONTEXT_CHANGED = 'cyys:context-changed';
let stored: StoredSession;
try { stored = JSON.parse(sessionStorage.getItem(KEY) || '{}'); }
catch { stored = {}; sessionStorage.removeItem(KEY); }
let revision = 0;
let dirty = false;

export const session = () => ({ ...stored, revision });
export const setDirty = (value: boolean) => { dirty = value; };
export const hasUnsavedChanges = () => dirty;
export const advanceRevision = () => ++revision;

export function updateSession(value: StoredSession) {
  stored = value;
  dirty = false;
  revision++;
  sessionStorage.setItem(KEY, JSON.stringify(value));
  window.dispatchEvent(new Event(CONTEXT_CHANGED));
}
export const clearSession = () => updateSession({});
export const refreshIdentity = () => {
  revision++;
  dirty = false;
  window.dispatchEvent(new Event(CONTEXT_CHANGED));
};
