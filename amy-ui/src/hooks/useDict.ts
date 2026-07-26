import { request } from '@umijs/max';
import { useEffect, useState } from 'react';
import type { RuoYiResponse } from '@/types/api';

export interface DictItem {
  dictCode: number;
  dictLabel: string;
  dictValue: string;
  listClass?: string;
  cssClass?: string;
}

export function useDict(dictType: string) {
  const [items, setItems] = useState<DictItem[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    request<RuoYiResponse<DictItem[]>>(`/system/dict/data/type/${dictType}`)
      .then((result) => setItems(result.data || []))
      .finally(() => setLoading(false));
  }, [dictType]);

  return {
    items,
    loading,
    options: items.map((item) => ({ label: item.dictLabel, value: item.dictValue }))
  };
}
