import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { useAuth } from '../../auth/AuthContext';
import {
  createUser,
  fetchRoles,
  fetchUser,
  fetchUsers,
  updateUser,
  updateUserRoles,
  updateUserStatus,
} from '../../services/usersService';
import type { CreatedUser, ManagedUser, RoleOption } from '../../types/api';
import { formatRoleList, formatRoleName } from '../../utils/formatRoleName';

export function UsersPage() {
  const { hasPermission } = useAuth();
  const canManage = hasPermission('user:manage');

  const [users, setUsers] = useState<ManagedUser[]>([]);
  const [roles, setRoles] = useState<RoleOption[]>([]);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [selectedUser, setSelectedUser] = useState<ManagedUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [createdCredentials, setCreatedCredentials] = useState<CreatedUser | null>(null);
  const [createForm, setCreateForm] = useState({
    firstName: '',
    lastName: '',
    roleCodes: [] as string[],
  });

  const loadUsers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetchUsers(search, page, 20);
      setUsers(response.items);
      setTotalPages(response.totalPages);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load users');
    } finally {
      setLoading(false);
    }
  }, [page, search]);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  useEffect(() => {
    fetchRoles().then(setRoles).catch(() => {});
  }, []);

  useEffect(() => {
    if (selectedId == null) {
      setSelectedUser(null);
      return;
    }
    fetchUser(selectedId).then(setSelectedUser).catch((err) => {
      setError(err instanceof Error ? err.message : 'Failed to load user');
    });
  }, [selectedId]);

  async function handleCreate(event: FormEvent) {
    event.preventDefault();
    try {
      const created = await createUser({
        firstName: createForm.firstName.trim(),
        lastName: createForm.lastName.trim(),
        roleCodes: createForm.roleCodes.length > 0 ? createForm.roleCodes : undefined,
      });
      setShowCreate(false);
      setCreateForm({ firstName: '', lastName: '', roleCodes: [] });
      setCreatedCredentials(created);
      setSelectedId(created.id);
      loadUsers();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create user');
    }
  }

  async function saveUserDetails() {
    if (!selectedUser || !canManage) {
      return;
    }
    try {
      const updated = await updateUser(selectedUser.id, {
        firstName: selectedUser.firstName,
        lastName: selectedUser.lastName,
        phone: selectedUser.phone ?? undefined,
      });
      setSelectedUser(updated);
      loadUsers();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update user');
    }
  }

  async function toggleRole(roleCode: string) {
    if (!selectedUser || !canManage) {
      return;
    }
    const next = selectedUser.roles.includes(roleCode)
      ? selectedUser.roles.filter((r) => r !== roleCode)
      : [...selectedUser.roles, roleCode];
    try {
      const updated = await updateUserRoles(selectedUser.id, next);
      setSelectedUser(updated);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update roles');
    }
  }

  async function setStatus(status: string) {
    if (!selectedUser || !canManage) {
      return;
    }
    try {
      const updated = await updateUserStatus(selectedUser.id, status);
      setSelectedUser(updated);
      loadUsers();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update status');
    }
  }

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="eyebrow">Administration</p>
          <h1>Users</h1>
          <p className="subtitle">Manage team accounts and roles</p>
        </div>
        {canManage && (
          <div className="page__header-actions">
            <button
              type="button"
              className="btn btn--primary"
              onClick={() => {
                setShowCreate((v) => !v);
                setCreatedCredentials(null);
              }}
            >
              {showCreate ? 'Cancel' : 'New user'}
            </button>
          </div>
        )}
      </header>

      <div className="toolbar">
        <input
          type="search"
          className="input"
          placeholder="Search users…"
          value={search}
          onChange={(e) => { setPage(0); setSearch(e.target.value); }}
        />
      </div>

      {createdCredentials && (
        <section className="panel panel--success">
          <h2>User created</h2>
          <p className="muted">Share these login details with the new team member.</p>
          <dl className="detail-list">
            <div><dt>Name</dt><dd>{createdCredentials.fullName}</dd></div>
            <div><dt>Email</dt><dd>{createdCredentials.email}</dd></div>
            <div><dt>Username</dt><dd>{createdCredentials.username}</dd></div>
            {createdCredentials.generatedPassword && (
              <div><dt>Password</dt><dd><code>{createdCredentials.generatedPassword}</code></dd></div>
            )}
          </dl>
          <button type="button" className="btn btn--ghost" onClick={() => setCreatedCredentials(null)}>
            Dismiss
          </button>
        </section>
      )}

      {showCreate && canManage && (
        <section className="panel">
          <h2>Create user</h2>
          <p className="muted">Enter the person&apos;s name. Email, username, and password are generated automatically.</p>
          <form className="form form--touch-friendly" onSubmit={handleCreate}>
            <label className="form__field">
              <span>First name</span>
              <input className="input" required value={createForm.firstName} onChange={(e) => setCreateForm({ ...createForm, firstName: e.target.value })} />
            </label>
            <label className="form__field">
              <span>Last name</span>
              <input className="input" required value={createForm.lastName} onChange={(e) => setCreateForm({ ...createForm, lastName: e.target.value })} />
            </label>
            <div className="form__field form__field--wide">
              <span>Roles</span>
              <div className="role-grid">
                {roles.filter((r) => r.code !== 'OWNER').map((role) => (
                  <label key={role.code} className="checkbox">
                    <input
                      type="checkbox"
                      checked={createForm.roleCodes.includes(role.code)}
                      onChange={() => {
                        const next = createForm.roleCodes.includes(role.code)
                          ? createForm.roleCodes.filter((c) => c !== role.code)
                          : [...createForm.roleCodes, role.code];
                        setCreateForm({ ...createForm, roleCodes: next });
                      }}
                    />
                    {formatRoleName(role.code)}
                  </label>
                ))}
              </div>
              <p className="muted">Defaults to Shop worker if none selected.</p>
            </div>
            <div className="form__field form__field--wide">
              <button type="submit" className="btn btn--primary">Create user</button>
            </div>
          </form>
        </section>
      )}

      {loading && <p className="muted">Loading users…</p>}
      {error && <p className="form__error">{error}</p>}

      {!loading && (
        <div className={`workspace-split${selectedId != null ? ' workspace-split--open' : ''}`}>
          <div className="workspace-split__list">
            <div className="table-wrap table-wrap--stacked table-wrap--scroll-hint">
              <table className="table table--stacked">
                <thead><tr><th>Name</th><th>Email</th><th>Status</th><th>Roles</th></tr></thead>
                <tbody>
                  {users.map((user) => (
                    <tr key={user.id} className={`table__row--clickable${selectedId === user.id ? ' table__row--selected' : ''}`} onClick={() => setSelectedId(user.id)}>
                      <td><strong>{user.fullName}</strong></td>
                      <td>{user.email}</td>
                      <td>{user.status}</td>
                      <td>{formatRoleList(user.roles)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {totalPages > 1 && (
              <div className="pager">
                <button type="button" className="btn btn--ghost" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Previous</button>
                <span className="muted">Page {page + 1} of {totalPages}</span>
                <button type="button" className="btn btn--ghost" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>Next</button>
              </div>
            )}
          </div>

          {selectedUser && (
            <aside className="workspace-split__detail panel">
              <h2>{selectedUser.fullName}</h2>
              <p className="muted">{selectedUser.email} · @{selectedUser.username}</p>
              {canManage && (
                <>
                  <label className="form__field"><span>First name</span><input className="input" value={selectedUser.firstName} onChange={(e) => setSelectedUser({ ...selectedUser, firstName: e.target.value })} /></label>
                  <label className="form__field"><span>Last name</span><input className="input" value={selectedUser.lastName} onChange={(e) => setSelectedUser({ ...selectedUser, lastName: e.target.value })} /></label>
                  <button type="button" className="btn btn--ghost" onClick={saveUserDetails}>Save details</button>
                  <h3 className="panel__subheading">Roles</h3>
                  <div className="role-grid">
                    {roles.map((role) => (
                      <label key={role.code} className="checkbox">
                        <input type="checkbox" checked={selectedUser.roles.includes(role.code)} onChange={() => toggleRole(role.code)} />
                        {formatRoleName(role.code)}
                      </label>
                    ))}
                  </div>
                  <h3 className="panel__subheading">Status</h3>
                  <div className="page__header-actions">
                    {selectedUser.status !== 'ACTIVE' && <button type="button" className="btn btn--ghost" onClick={() => setStatus('ACTIVE')}>Activate</button>}
                    {selectedUser.status === 'ACTIVE' && <button type="button" className="btn btn--ghost" onClick={() => setStatus('INACTIVE')}>Deactivate</button>}
                  </div>
                </>
              )}
            </aside>
          )}
        </div>
      )}
    </div>
  );
}
