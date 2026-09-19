import {useEffect, useId, useMemo, useRef, useState} from 'react';

/** 折线图数据点 */
export interface LineChartPoint {
    /** X 轴标签，如 08/28 */
    label: string;
    /** 数值 */
    value: number;
    /** 悬浮提示标题，默认取 label */
    title?: string;
}

export interface LineChartProps {
    data: LineChartPoint[];
    /** 折线颜色 */
    color?: string;
    /** 图表高度 */
    height?: number;
    /** 悬浮提示单位 */
    unit?: string;
}

type Point = [number, number];

/** 图表内边距（用于坐标轴刻度） */
const PADDING = {top: 16, right: 18, bottom: 30, left: 56};

/** X 轴最多展示的刻度数量 */
const MAX_X_TICKS = 8;

/** 计算「整齐」的 Y 轴最大值与步长 */
function niceScale(maxValue: number, tickCount = 6) {
    const max = Math.max(maxValue, 1);
    const rawStep = max / tickCount;
    const magnitude = Math.pow(10, Math.floor(Math.log10(rawStep)));
    const normalized = rawStep / magnitude;
    let step: number;
    if (normalized <= 1) step = 1;
    else if (normalized <= 2) step = 2;
    else if (normalized <= 5) step = 5;
    else step = 10;
    step *= magnitude;
    return {max: Math.ceil(max / step) * step, step};
}

/**
 * 单调三次插值（对齐 ECharts smooth 曲线）：
 * 相比 Catmull-Rom 不会在极值附近产生「过冲」，曲线更贴合真实数据。
 */
function monotonePath(points: Point[]): string {
    const n = points.length;
    if (n === 0) return '';
    const first = points[0]!;
    if (n === 1) return `M ${first[0]} ${first[1]}`;
    const second = points[1]!;
    if (n === 2) return `M ${first[0]} ${first[1]} L ${second[0]} ${second[1]}`;

    const dx: number[] = [];
    const slope: number[] = [];
    for (let i = 0; i < n - 1; i++) {
        const a = points[i]!;
        const b = points[i + 1]!;
        const stepX = b[0] - a[0] || 1;
        dx.push(stepX);
        slope.push((b[1] - a[1]) / stepX);
    }

    const tangent: number[] = [slope[0]!];
    for (let i = 1; i < n - 1; i++) {
        const prevSlope = slope[i - 1]!;
        const currSlope = slope[i]!;
        if (prevSlope * currSlope <= 0) {
            tangent[i] = 0;
        } else {
            const w1 = 2 * dx[i]! + dx[i - 1]!;
            const w2 = dx[i]! + 2 * dx[i - 1]!;
            tangent[i] = (w1 + w2) / (w1 / prevSlope + w2 / currSlope);
        }
    }
    tangent[n - 1] = slope[n - 2]!;

    let d = `M ${first[0]} ${first[1]}`;
    for (let i = 0; i < n - 1; i++) {
        const a = points[i]!;
        const b = points[i + 1]!;
        const h = dx[i]!;
        const c1x = a[0] + h / 3;
        const c1y = a[1] + (tangent[i]! * h) / 3;
        const c2x = b[0] - h / 3;
        const c2y = b[1] - (tangent[i + 1]! * h) / 3;
        d += ` C ${c1x} ${c1y} ${c2x} ${c2y} ${b[0]} ${b[1]}`;
    }
    return d;
}

const formatNumber = (value: number) => value.toLocaleString('en-US');

/**
 * 轻量 SVG 折线图（无第三方依赖）。
 * 支持自适应宽度、Y 轴整齐刻度、X 轴稀疏刻度、悬浮虚线 + 数据提示。
 */
export default function LineChart({data, color = '#4096ff', height = 300, unit = ''}: LineChartProps) {
    const wrapRef = useRef<HTMLDivElement>(null);
    const [width, setWidth] = useState(0);
    const [hoverIndex, setHoverIndex] = useState(-1);
    // React useId 生成的 id 含 ':'，部分浏览器解析 SVG 渐变引用时会异常，这里做一次清洗
    const gradientId = `line-area-${useId().replace(/:/g, '')}`;

    useEffect(() => {
        const el = wrapRef.current;
        if (!el) return;
        const update = () => setWidth(el.clientWidth);
        update();
        const observer = new ResizeObserver(update);
        observer.observe(el);
        return () => observer.disconnect();
    }, []);

    const innerWidth = Math.max(width - PADDING.left - PADDING.right, 10);
    const innerHeight = Math.max(height - PADDING.top - PADDING.bottom, 10);

    const maxValue = data.reduce((acc, item) => Math.max(acc, item.value || 0), 0);
    const {max, step} = useMemo(() => niceScale(maxValue), [maxValue]);
    const tickCount = Math.round(max / step);

    const points = useMemo<Point[]>(() => data.map((item, i) => {
        const ratio = data.length <= 1 ? 0.5 : i / (data.length - 1);
        const x = PADDING.left + ratio * innerWidth;
        const y = PADDING.top + innerHeight - ((item.value || 0) / max) * innerHeight;
        return [x, y];
    }), [data, innerWidth, innerHeight, max]);

    const linePath = monotonePath(points);
    const baseline = PADDING.top + innerHeight;
    const firstPoint = points[0];
    const lastPoint = points[points.length - 1];
    const areaPath = firstPoint && lastPoint
        ? `${linePath} L ${lastPoint[0]} ${baseline} L ${firstPoint[0]} ${baseline} Z`
        : '';

    const xTickStep = Math.max(1, Math.ceil(data.length / MAX_X_TICKS));
    const activeIndex = hoverIndex >= 0 && hoverIndex < data.length ? hoverIndex : -1;
    const hover = activeIndex >= 0 ? {point: points[activeIndex]!, item: data[activeIndex]!} : undefined;

    const handleMove = (event: React.MouseEvent<SVGSVGElement>) => {
        if (!points.length) return;
        const rect = event.currentTarget.getBoundingClientRect();
        const ratio = (event.clientX - rect.left - PADDING.left) / innerWidth;
        const index = Math.round(ratio * (data.length - 1));
        setHoverIndex(Math.min(Math.max(index, 0), data.length - 1));
    };

    return (
        <div ref={wrapRef} style={{position: 'relative', width: '100%', height}}>
            {width > 0 && (
                <svg
                    width={width}
                    height={height}
                    style={{display: 'block'}}
                    onMouseMove={handleMove}
                    onMouseLeave={() => setHoverIndex(-1)}
                >
                    <defs>
                        <linearGradient id={`${gradientId}-area`} x1="0" y1="0" x2="0" y2="1">
                            <stop offset="0%" stopColor={color} stopOpacity={0.16}/>
                            <stop offset="100%" stopColor={color} stopOpacity={0}/>
                        </linearGradient>
                    </defs>

                    {/* Y 轴网格线与刻度 */}
                    {Array.from({length: tickCount + 1}, (_, i) => {
                        const value = i * step;
                        const y = baseline - (value / max) * innerHeight;
                        return (
                            <g key={`y-${i}`}>
                                <line x1={PADDING.left} y1={y} x2={PADDING.left + innerWidth} y2={y}
                                      stroke="#f0f0f0" strokeWidth={1}/>
                                <text x={PADDING.left - 10} y={y + 4} textAnchor="end" fontSize={12} fill="#8c8c8c">
                                    {formatNumber(value)}
                                </text>
                            </g>
                        );
                    })}

                    {/* X 轴刻度 */}
                    {data.map((item, i) => {
                        if (i % xTickStep !== 0) return null;
                        const point = points[i];
                        if (!point) return null;
                        return (
                            <text key={`x-${item.label}-${i}`} x={point[0]} y={height - 10}
                                  textAnchor="middle" fontSize={12} fill="#8c8c8c">
                                {item.label}
                            </text>
                        );
                    })}

                    {areaPath && <path d={areaPath} fill={`url(#${gradientId}-area)`}/>}
                    <path d={linePath} fill="none" stroke={color} strokeWidth={2}
                          strokeLinecap="round" strokeLinejoin="round"/>

                    {hover && (
                        <g>
                            <line x1={hover.point[0]} y1={PADDING.top} x2={hover.point[0]} y2={baseline}
                                  stroke="#d9d9d9" strokeDasharray="4 4"/>
                            <circle cx={hover.point[0]} cy={hover.point[1]} r={4} fill="#fff"
                                    stroke={color} strokeWidth={2}/>
                        </g>
                    )}
                </svg>
            )}

            {hover && (
                <div style={{
                    position: 'absolute',
                    left: hover.point[0],
                    top: hover.point[1],
                    transform: 'translate(-50%, calc(-100% - 12px))',
                    background: 'rgba(0,0,0,0.75)',
                    color: '#fff',
                    padding: '6px 10px',
                    borderRadius: 4,
                    fontSize: 12,
                    lineHeight: 1.5,
                    whiteSpace: 'nowrap',
                    pointerEvents: 'none'
                }}>
                    <div>{hover.item.title || hover.item.label}</div>
                    <div style={{display: 'flex', alignItems: 'center', gap: 6}}>
                        <span style={{
                            display: 'inline-block',
                            width: 6,
                            height: 6,
                            borderRadius: '50%',
                            background: color
                        }}/>
                        {formatNumber(hover.item.value)}{unit}
                    </div>
                </div>
            )}
        </div>
    );
}
