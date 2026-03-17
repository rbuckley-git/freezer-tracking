'use client';

import { useMemo, useState } from 'react';

type Account = {
  id: string;
  username: string;
  isAdmin: boolean;
  isSuperAdmin: boolean;
  houseId: string;
};

type AccountsTableProps = {
  accounts: Account[];
  houses: { id: string; name: string }[];
  canManageSuperAdmins: boolean;
  onUpdate: (formData: FormData) => void;
  onDelete: (formData: FormData) => void;
};

function AccountRow({
  account,
  houses,
  canManageSuperAdmins,
  onUpdate,
  onDelete
}: {
  account: Account;
  houses: AccountsTableProps['houses'];
  canManageSuperAdmins: AccountsTableProps['canManageSuperAdmins'];
  onUpdate: AccountsTableProps['onUpdate'];
  onDelete: AccountsTableProps['onDelete'];
}) {
  const [username, setUsername] = useState(account.username);
  const [isAdmin, setIsAdmin] = useState(account.isAdmin);
  const [isSuperAdmin, setIsSuperAdmin] = useState(account.isSuperAdmin);
  const [houseId, setHouseId] = useState(account.houseId);

  const isDirty = useMemo(
    () =>
      username !== account.username
      || isAdmin !== account.isAdmin
      || isSuperAdmin !== account.isSuperAdmin
      || houseId !== account.houseId,
    [username, isAdmin, isSuperAdmin, houseId, account]
  );

  return (
    <tr>
      <td>
        <input
          name="username"
          type="email"
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          required
        />
      </td>
      <td>
        <label className="radio-pill">
          <input
            type="checkbox"
            name="isAdmin"
            checked={isAdmin}
            onChange={(event) => {
              const nextIsAdmin = event.target.checked;
              setIsAdmin(nextIsAdmin);
              if (!nextIsAdmin) {
                setIsSuperAdmin(false);
              }
            }}
          />
        </label>
      </td>
      <td>
        {canManageSuperAdmins ? (
          <label className="radio-pill">
            <input
              type="checkbox"
              name="isSuperAdmin"
              checked={isSuperAdmin}
              onChange={(event) => {
                const nextIsSuperAdmin = event.target.checked;
                setIsSuperAdmin(nextIsSuperAdmin);
                if (nextIsSuperAdmin) {
                  setIsAdmin(true);
                }
              }}
            />
          </label>
        ) : (
          account.isSuperAdmin ? 'Yes' : 'No'
        )}
      </td>
      <td>
        <select name="houseId" value={houseId} onChange={(event) => setHouseId(event.target.value)}>
          {houses.map((house) => (
            <option key={house.id} value={house.id}>
              {house.name}
            </option>
          ))}
        </select>
      </td>
      <td>
        <div className="actions flex-nowrap">
          <form action={onUpdate}>
            <input type="hidden" name="id" value={account.id} />
            <input type="hidden" name="username" value={username} />
            <input type="hidden" name="isAdmin" value={isAdmin ? 'on' : ''} />
            <input type="hidden" name="isSuperAdmin" value={isSuperAdmin ? 'on' : ''} />
            <input type="hidden" name="houseId" value={houseId} />
            <button type="submit" className="secondary" disabled={!isDirty}>Save</button>
          </form>
          <form action={onDelete}>
            <input type="hidden" name="id" value={account.id} />
            <button type="submit" className="secondary">Delete</button>
          </form>
        </div>
      </td>
    </tr>
  );
}

export default function AccountsTable({
  accounts,
  houses,
  canManageSuperAdmins,
  onUpdate,
  onDelete
}: AccountsTableProps) {
  return (
    <table className="table mt-16">
      <thead>
        <tr>
          <th>Email</th>
          <th>Admin</th>
          <th>Super admin</th>
          <th>House</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {accounts.map((account) => (
          <AccountRow
            key={account.id}
            account={account}
            houses={houses}
            canManageSuperAdmins={canManageSuperAdmins}
            onUpdate={onUpdate}
            onDelete={onDelete}
          />
        ))}
      </tbody>
    </table>
  );
}
