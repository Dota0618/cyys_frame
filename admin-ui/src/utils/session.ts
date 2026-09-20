interface StoredSession { token?: string; scopeId?: string; allUnits?: boolean }
const KEY = 'cyys.session';
export const CONTEXT_CHANGED = 'cyys:context-changed';
let stored: StoredSession;
try { stored = JSON.parse(sessionStorage.getItem(KEY) || '{}'); }
catch { stored = {}; sessionStorage.removeItem(KEY); }
let revision = 0;
const dirtyOwners = new Set<string>();

export const session = () => ({ ...stored, revision });
export const setDirty = (value: boolean, owner = 'page') => { if (value) dirtyOwners.add(owner); else dirtyOwners.delete(owner); };
export const hasUnsavedChanges = () => dirtyOwners.size > 0;
export const advanceRevision = () => ++revision;

export function updateSession(value: StoredSession) {
  stored = value;
  dirtyOwners.clear();
  revision++;
  sessionStorage.setItem(KEY, JSON.stringify(value));
  window.dispatchEvent(new Event(CONTEXT_CHANGED));
}
export const clearSession = () => updateSession({});
export const refreshIdentity = () => {
  revision++;
  dirtyOwners.clear();
  window.dispatchEvent(new Event(CONTEXT_CHANGED));
};
