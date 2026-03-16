import { useMemo } from 'react';
import { useLogoAssets } from '@app/hooks/useLogoAssets';

/**
 * Hook to get the correct logo path based on app config and theme.
 * Y-Edit uses the Y-12 logo (PNG) instead of Stirling SVG wordmarks.
 */
export function useLogoPath(): string {
  const { folderPath } = useLogoAssets();

  return useMemo(() => {
    return `${folderPath}/logo192.png`;
  }, [folderPath]);
}
