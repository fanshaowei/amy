import {useEffect, useRef, useState} from 'react';

export interface CountUpProps {
    /** 目标数值 */
    value: number;
    /** 动画时长（毫秒） */
    duration?: number;
}

/**
 * 数值滚动动画（easeOutCubic）。
 * 目标值变化时从 0 重新滚动到目标值，用于控制台指标卡片的数字展示。
 */
export function CountUp({value, duration = 1100}: CountUpProps) {
    const [display, setDisplay] = useState(0);
    const frameRef = useRef<number | null>(null);

    useEffect(() => {
        const target = Number.isFinite(value) ? Math.max(value, 0) : 0;
        const startAt = performance.now();

        const tick = (now: number) => {
            const progress = Math.min((now - startAt) / duration, 1);
            const eased = 1 - Math.pow(1 - progress, 3);
            setDisplay(Math.round(target * eased));
            if (progress < 1) {
                frameRef.current = requestAnimationFrame(tick);
            }
        };

        frameRef.current = requestAnimationFrame(tick);
        return () => {
            if (frameRef.current !== null) {
                cancelAnimationFrame(frameRef.current);
            }
        };
    }, [value, duration]);

    return <>{display.toLocaleString('en-US')}</>;
}

export default CountUp;
