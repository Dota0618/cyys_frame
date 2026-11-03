interface StoredSession { token?: string; scopeId?: string }
const KEY = 'cyys.session';
export const CONTEXT_CHANGED = 'cyys:context-changed';
export const AUTHORIZATION_CHANGED = 'cyys:authorization-changed';
export const DIRTY_CHANGED = 'cyys:dirty-changed';
let stored: StoredSession;
try { stored = JSON.parse(sessionStorage.getItem(KEY) || '{}'); }
catch { stored = {}; sessionStorage.removeItem(KEY); }
let revision = 0;
const dirtyOwners = new Set<string>();

export const session = () => ({ ...stored, revision });
export const setDirty = (value: boolean, owner = 'page') => {
  const previous = dirtyOwners.size > 0;
  if (value) dirtyOwners.add(owner); else dirtyOwners.delete(owner);
  if (previous !== (dirtyOwners.size > 0)) window.dispatchEvent(new Event(DIRTY_CHANGED));
};
export const hasUnsavedChanges = () => dirtyOwners.size > 0;
export const advanceRevision = () => {
  ++revision;
  window.dispatchEvent(new Event(AUTHORIZATION_CHANGED));
  return revision;
};

export function updateSession(value: StoredSession) {
  stored = value;
  dirtyOwners.clear();
  window.dispatchEvent(new Event(DIRTY_CHANGED));
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
