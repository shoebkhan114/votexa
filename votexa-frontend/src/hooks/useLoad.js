import { useCallback, useEffect, useState } from 'react';
import { errMsg } from '../services/api.js';

/** Loads data from an async function; returns { data, loading, error, reload }. */
export default function useLoad(fn, deps = [], intervalMs = 0) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const run = useCallback(async (silent) => {
    if (!silent) setLoading(true);
    try { setData(await fn()); setError(''); } catch (e) { setError(errMsg(e)); }
    finally { setLoading(false); }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => {
    run(false);
    if (!intervalMs) return undefined;
    const t = setInterval(() => run(true), intervalMs);
    return () => clearInterval(t);
  }, [run, intervalMs]);

  return { data, loading, error, reload: () => run(true) };
}
