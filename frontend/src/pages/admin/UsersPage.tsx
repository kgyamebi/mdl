import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { useAuth } from '../../auth/AuthContext';
import { fetchShops } from '../../services/locationsService';
import {
  createUser,
  fetchRoles,
  fetchUser,
  fetchUsers,
  updateUser,
  updateUserLocations,
  updateUserRoles,
  updateUserStatus,
} from '../../services/usersService';
import type { CreatedUser, ManagedUser, RoleOption, Shop } from '../../types/api';
import { formatRoleList, formatRoleName } from '../../utils/formatRoleName';

const SHOP_SELLING_ROLES = new Set(['SHOP_WORKER', 'SHOP_MANAGER', 'SALES_STAFF']);

function rolesNeedShop(roleCodes: string[]): boolean {
  const roles = roleCodes.length > 0 ? roleCodes : ['SHOP_WORKER'];
  return roles.some((code) => SHOP_SELLING_ROLES.has(code));
}

function locationIdsForShops(shops: Shop[], shopIds: number[]): number[] {
  const ids = new Set<number>();
  for (const shop of shops) {
    if (!shopIds.includes(shop.id)) {
      continue;
    }
    if (shop.location?.id) {
      ids.add(shop.location.id);
    }
    if (shop.warehouseLocationId) {
      ids.add(shop.warehouseLocationId);
    }
  }
  return [...ids];
}

function assignedShopIds(user: ManagedUser, shops: Shop[]): number[] {
  const assigned = new Set(user.locations.map((location) => location.locationId));
  return shops
    .filter((shop) => shop.location?.id != null && assigned.has(shop.location.id))
    .map((shop) => shop.id);
}

function shopNamesForUser(user: ManagedUser): string {
  const shopLocations = user.locations.filter(
    (location) => !location.locationType || location.locationType === 'SHOP',
  );
  if (shopLocations.length === 0) {
    return 'None';
  }
  return shopLocations.map((location) => location.locationName).join(', ');
}

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
    shopIds: [] as number[],
  });
  const [shops, setShops] = useState<Shop[]>([]);
  const [detailShopIds, setDetailShopIds] = useState<number[]>([]);

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
    fetchShops()
      .then((shopList) => setShops(shopList.filter((shop) => shop.status === 'ACTIVE')))
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (selectedId == null) {
      setSelectedUser(null);
      setDetailShopIds([]);
      return;
    }
    fetchUser(selectedId)
      .then((user) => {
        setSelectedUser(user);
        setDetailShopIds(assignedShopIds(user, shops));
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : 'Failed to load user');
      });
  }, [selectedId, shops]);

  async function handleCreate(event: FormEvent) {
    event.preventDefault();
    if (rolesNeedShop(createForm.roleCodes) && createForm.shopIds.length === 0) {
      setError('Assign at least one shop so this person can make sales.');
      return;
    }
    try {
      const created = await createUser({
        firstName: createForm.firstName.trim(),
        lastName: createForm.lastName.trim(),
        roleCodes: createForm.roleCodes.length > 0 ? createForm.roleCodes : undefined,
        locationIds: locationIdsForShops(shops, createForm.shopIds),
      });
      setShowCreate(false);
      setCreateForm({ firstName: '', lastName: '', roleCodes: [], shopIds: [] });
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

  async function saveShopAssignments() {
    if (!selectedUser || !canManage) {
      return;
    }
    if (rolesNeedShop(selectedUser.roles) && detailShopIds.length === 0) {
      setError('Assign at least one shop so this person can make sales.');
      return;
    }
    try {
      const updated = await updateUserLocations(
        selectedUser.id,
        locationIdsForShops(shops, detailShopIds).map((locationId) => ({
          locationId,
          accessLevel: 'FULL',
        })),
      );
      setSelectedUser(updated);
      setDetailShopIds(assignedShopIds(updated, shops));
      loadUsers();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to assign shops');
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
          <p className="subtitle">Manage team accounts, roles, and which shop they sell from</p>
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
          <p className="muted">Enter the person&apos;s name and the shop they work at. Email, username, and password are generated automatically.</p>
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
            <fieldset className="form__field form__field--wide">
              <legend>Shops they can sell from</legend>
              <div className="role-grid">
                {shops.map((shop) => (
                  <label key={shop.id} className="checkbox">
                    <input
                      type="checkbox"
                      checked={createForm.shopIds.includes(shop.id)}
                      onChange={() => {
                        const next = createForm.shopIds.includes(shop.id)
                          ? createForm.shopIds.filter((id) => id !== shop.id)
                          : [...createForm.shopIds, shop.id];
                        setCreateForm({ ...createForm, shopIds: next });
                      }}
                    />
                    {shop.name}
                  </label>
                ))}
              </div>
              <p className="muted">
                Required for shop workers. They can look up stock at every shop, but can only complete sales at assigned shops.
              </p>
            </fieldset>
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
                <thead><tr><th>Name</th><th>Email</th><th>Status</th><th>Roles</th><th>Shops</th></tr></thead>
                <tbody>
                  {users.map((user) => (
                    <tr key={user.id} className={`table__row--clickable${selectedId === user.id ? ' table__row--selected' : ''}`} onClick={() => setSelectedId(user.id)}>
                      <td><strong>{user.fullName}</strong></td>
                      <td>{user.email}</td>
                      <td>{user.status}</td>
                      <td>{formatRoleList(user.roles)}</td>
                      <td>{shopNamesForUser(user)}</td>
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
                  <fieldset>
                    <legend className="panel__subheading">Shops they can sell from</legend>
                    <div className="role-grid">
                      {shops.map((shop) => (
                        <label key={shop.id} className="checkbox">
                          <input
                            type="checkbox"
                            checked={detailShopIds.includes(shop.id)}
                            onChange={() => {
                              setDetailShopIds((current) =>
                                current.includes(shop.id)
                                  ? current.filter((id) => id !== shop.id)
                                  : [...current, shop.id],
                              );
                            }}
                          />
                          {shop.name}
                        </label>
                      ))}
                    </div>
                    <button type="button" className="btn btn--ghost" onClick={saveShopAssignments}>
                      Save shop assignment
                    </button>
                  </fieldset>
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
