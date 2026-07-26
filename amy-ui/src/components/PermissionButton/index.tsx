import { useAccess } from '@umijs/max';
import { Button } from 'antd';
import type { ButtonProps } from 'antd';

interface PermissionButtonProps extends ButtonProps {
  permission: string;
}

export function PermissionButton({ permission, ...props }: PermissionButtonProps) {
  const access = useAccess();
  if (!access.hasPermission(permission)) return null;
  return <Button {...props} />;
}
