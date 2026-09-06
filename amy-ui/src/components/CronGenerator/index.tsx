import {ClockCircleOutlined, RedoOutlined} from '@ant-design/icons';
import {Button, Checkbox, InputNumber, Modal, Radio, Select, Tabs, Tooltip} from 'antd';
import {useEffect, useMemo, useState} from 'react';
import {nextRuns} from './cron';

/**
 * cron 表达式生成器
 *
 * 复刻 ruoyi-ui 中 components/Crontab 的功能：
 *   - 秒/分/时/日/月/周/年 七个标签页
 *   - 每个标签页提供 4 种规则：每、周期 X-Y、从 X 起每 Y、指定值
 *   - 日 / 周 含 W / L / # 等扩展规则
 *   - 实时展示组装好的 cron 串，以及接下来 5 次执行时间
 *
 * 父组件通过 value/onChange 双向绑定；最终填回使用 Modal 的"确定"按钮。
 */
export interface CronGeneratorProps {
    /** 当前 cron 表达式（用于反解析回填到表单） */
    value?: string;
    /** cron 表达式变化时的回调（实时同步） */
    onChange?: (v: string) => void;
    /** Modal 受控打开状态 */
    open: boolean;
    /** 点击"确定"后回填到表单并关闭 */
    onOk?: (v: string) => void;
    /** 取消关闭 */
    onCancel?: () => void;
    /** 标题 */
    title?: string;
    /** 隐藏某些标签；'second' 隐藏秒 */
    hideTabs?: Array<'second' | 'min' | 'hour' | 'day' | 'month' | 'week' | 'year'>;
}

type CronObj = {
    second: string;
    min: string;
    hour: string;
    day: string;
    month: string;
    week: string;
    year: string;
};

const DEFAULT_CRON: CronObj = {second: '*', min: '*', hour: '*', day: '*', month: '*', week: '?', year: ''};

/** 把 cron 串拆成 7 段；段数不足时按 Quartz 默认补齐 */
function splitCron(expression: string): CronObj {
    if (!expression) return {...DEFAULT_CRON};
    const parts = expression.trim().split(/\s+/);
    const [second = '*', min = '*', hour = '*', day = '*', month = '*', week = '?', year = ''] = parts;
    return {second, min, hour, day, month, week, year};
}

/** 把 7 段组装成 cron 串（year 为空则省略） */
function joinCron(c: CronObj): string {
    return [c.second, c.min, c.hour, c.day, c.month, c.week, c.year].filter((s, i) => i < 6 || s !== '').join(' ');
}

/**
 * 反向解析 cron 字段，把 "* / X-Y / X/Y / a,b,c" 等规则映射到 radio + 表单值
 *   返回 radio 编号（1=每, 2=周期, 3=平均, 4=指定, 5/6/7=特殊）
 */
function parseField(value: string, kind: 'second' | 'min' | 'hour' | 'day' | 'month' | 'week' | 'year') {
    const defaultRadio = kind === 'week' ? 2 : kind === 'year' ? 1 : 1;
    if (!value || value === '*') return {radio: 1, cycle: [0, 1], average: [0, 1], list: [] as number[]};
    if (value === '?') return {radio: 2, cycle: [0, 1], average: [0, 1], list: [] as number[]};
    if (value.includes('-')) {
        const [a, b] = value.split('-').map(Number);
        return {radio: 2, cycle: [a, b], average: [0, 1], list: [] as number[]};
    }
    if (value.includes('/')) {
        const [a, b] = value.split('/').map(Number);
        return {radio: 3, cycle: [0, 1], average: [a, b], list: [] as number[]};
    }
    return {radio: 4, cycle: [0, 1], average: [0, 1], list: value.split(',').map(Number)};
}

/** 单个标签页基础属性 */
interface TabProps {
    cron: CronObj;
    set: (patch: Partial<CronObj>) => void;
}

/** "秒/分钟/小时" 标签页通用结构 */
function NumberTab(props: TabProps & {kind: 'second' | 'min' | 'hour' | 'month'; max: number}) {
    const {cron, set, kind, max} = props;
    const field = cron[kind];
    const init = parseField(field, kind);
    const [radio, setRadio] = useState(init.radio);
    const [cycle, setCycle] = useState<[number, number]>(init.cycle as [number, number]);
    const [avg, setAvg] = useState<[number, number]>(init.average as [number, number]);
    const [list, setList] = useState<number[]>(init.list);
    const low = kind === 'hour' ? 0 : 0;
    const high = max;

    useEffect(() => {
        if (radio === 1) set({[kind]: '*'});
        else if (radio === 2) set({[kind]: `${cycle[0]}-${cycle[1]}`});
        else if (radio === 3) set({[kind]: `${avg[0]}/${avg[1]}`});
        else set({[kind]: list.length ? list.join(',') : '*'});
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [radio, cycle[0], cycle[1], avg[0], avg[1], list.join(',')]);

    return (
        <div style={{padding: '8px 0'}}>
            <Radio.Group
                value={radio}
                onChange={(e) => {
                    setRadio(e.target.value);
                    if (e.target.value === 2 && cron.week === '?') set({week: kind === 'hour' ? '?' : '?'});
                }}
                style={{display: 'flex', flexDirection: 'column', gap: 10}}
            >
                <Radio value={1}>{kind === 'hour' ? '小时' : kind === 'min' ? '分钟' : '秒'}，允许的通配符[, - * /]</Radio>
                <Radio value={2}>
                    周期从{' '}
                    <InputNumber
                        size="small"
                        min={low}
                        max={high - 1}
                        value={cycle[0]}
                        onChange={(v) => {
                            const n = Number(v ?? 0);
                            setCycle([n, Math.max(n + 1, cycle[1])]);
                        }}
                    />{' '}
                    -{' '}
                    <InputNumber
                        size="small"
                        min={cycle[0] + 1}
                        max={high}
                        value={cycle[1]}
                        onChange={(v) => setCycle([cycle[0], Number(v ?? 0)])}
                    />{' '}
                    {kind === 'hour' ? '小时' : kind === 'min' ? '分钟' : '秒'}
                </Radio>
                <Radio value={3}>
                    从{' '}
                    <InputNumber
                        size="small"
                        min={low}
                        max={high - 1}
                        value={avg[0]}
                        onChange={(v) => setAvg([Number(v ?? 0), avg[1]])}
                    />{' '}
                    {kind === 'hour' ? '小时' : kind === 'min' ? '分钟' : '秒'}开始，每{' '}
                    <InputNumber
                        size="small"
                        min={1}
                        max={Math.max(1, high - avg[0])}
                        value={avg[1]}
                        onChange={(v) => setAvg([avg[0], Number(v ?? 1)])}
                    />{' '}
                    {kind === 'hour' ? '小时' : kind === 'min' ? '分钟' : '秒'}执行一次
                </Radio>
                <Radio value={4}>
                    指定{' '}
                    <Select
                        size="small"
                        mode="multiple"
                        style={{minWidth: 200}}
                        placeholder="可多选"
                        value={list}
                        onChange={(v) => setList(v as number[])}
                        options={Array.from({length: high + 1}, (_, i) => ({label: String(i), value: i}))}
                    />
                </Radio>
            </Radio.Group>
        </div>
    );
}

/** "月" 标签页：1-12 */
function MonthTab(props: TabProps) {
    return <NumberTab {...props} kind="month" max={12} />;
}

/** "年" 标签页：空=不指定 */
function YearTab({cron, set}: TabProps) {
    const field = cron.year;
    const init = parseField(field, 'year');
    const [radio, setRadio] = useState(field === '' ? 1 : init.radio);
    const [cycle, setCycle] = useState<[number, number]>(init.cycle as [number, number]);
    const [avg, setAvg] = useState<[number, number]>(init.average as [number, number]);
    const [list, setList] = useState<number[]>(init.list);
    const thisYear = new Date().getFullYear();

    useEffect(() => {
        if (radio === 1) set({year: ''});
        else if (radio === 2) set({year: '*'});
        else if (radio === 3) set({year: `${cycle[0]}-${cycle[1]}`});
        else if (radio === 4) set({year: `${avg[0]}/${avg[1]}`});
        else set({year: list.length ? list.join(',') : '*'});
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [radio, cycle[0], cycle[1], avg[0], avg[1], list.join(',')]);

    return (
        <div style={{padding: '8px 0'}}>
            <Radio.Group
                value={radio}
                onChange={(e) => setRadio(e.target.value)}
                style={{display: 'flex', flexDirection: 'column', gap: 10}}
            >
                <Radio value={1}>不指定（默认每年都执行）</Radio>
                <Radio value={2}>每年</Radio>
                <Radio value={3}>
                    周期从{' '}
                    <InputNumber size="small" min={thisYear} max={thisYear + 99} value={cycle[0]} onChange={(v) => setCycle([Number(v ?? 0), cycle[1]])} /> -{' '}
                    <InputNumber size="small" min={cycle[0]} max={thisYear + 100} value={cycle[1]} onChange={(v) => setCycle([cycle[0], Number(v ?? 0)])} /> 年
                </Radio>
                <Radio value={4}>
                    从{' '}
                    <InputNumber size="small" min={thisYear} max={thisYear + 99} value={avg[0]} onChange={(v) => setAvg([Number(v ?? 0), avg[1]])} /> 年开始，每{' '}
                    <InputNumber size="small" min={1} max={99} value={avg[1]} onChange={(v) => setAvg([avg[0], Number(v ?? 1)])} /> 年执行一次
                </Radio>
                <Radio value={5}>
                    指定{' '}
                    <Select
                        size="small"
                        mode="multiple"
                        style={{minWidth: 240}}
                        placeholder="可多选"
                        value={list}
                        onChange={(v) => setList(v as number[])}
                        options={Array.from({length: 101}, (_, i) => ({label: String(thisYear + i), value: thisYear + i}))}
                    />
                </Radio>
            </Radio.Group>
        </div>
    );
}

/** "日" 标签页：1-31 + W/L */
function DayTab({cron, set}: TabProps) {
    const field = cron.day;
    const init = parseField(field, 'day');
    const [radio, setRadio] = useState(
        field === '?' ? 2 : field.endsWith('W') ? 5 : field === 'L' ? 6 : init.radio
    );
    const [cycle, setCycle] = useState<[number, number]>(init.cycle as [number, number]);
    const [avg, setAvg] = useState<[number, number]>(init.average as [number, number]);
    const [list, setList] = useState<number[]>(init.list);
    const [workday, setWorkday] = useState<number>(field.endsWith('W') ? Number(field.replace('W', '')) || 1 : 1);

    useEffect(() => {
        if (radio === 1) {
            set({day: '*', week: '?'});
        } else if (radio === 2) {
            set({day: '?', week: '*'});
        } else if (radio === 3) {
            set({day: `${cycle[0]}-${cycle[1]}`});
        } else if (radio === 4) {
            set({day: `${avg[0]}/${avg[1]}`});
        } else if (radio === 5) {
            set({day: `${workday}W`, week: '?'});
        } else if (radio === 6) {
            set({day: 'L', week: '?'});
        } else {
            set({day: list.length ? list.join(',') : '*'});
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [radio, cycle[0], cycle[1], avg[0], avg[1], list.join(','), workday]);

    return (
        <div style={{padding: '8px 0'}}>
            <Radio.Group
                value={radio}
                onChange={(e) => setRadio(e.target.value)}
                style={{display: 'flex', flexDirection: 'column', gap: 10}}
            >
                <Radio value={1}>日，允许的通配符[, - * /]</Radio>
                <Radio value={2}>不指定（与"周"互斥，此时周必须指定）</Radio>
                <Radio value={3}>
                    周期从{' '}
                    <InputNumber size="small" min={1} max={30} value={cycle[0]} onChange={(v) => setCycle([Number(v ?? 0), cycle[1]])} /> -{' '}
                    <InputNumber size="small" min={cycle[0] + 1} max={31} value={cycle[1]} onChange={(v) => setCycle([cycle[0], Number(v ?? 0)])} /> 日
                </Radio>
                <Radio value={4}>
                    从{' '}
                    <InputNumber size="small" min={1} max={30} value={avg[0]} onChange={(v) => setAvg([Number(v ?? 0), avg[1]])} /> 日开始，每{' '}
                    <InputNumber size="small" min={1} max={30} value={avg[1]} onChange={(v) => setAvg([avg[0], Number(v ?? 1)])} /> 日执行一次
                </Radio>
                <Radio value={5}>
                    每月{' '}
                    <InputNumber size="small" min={1} max={31} value={workday} onChange={(v) => setWorkday(Number(v ?? 1))} /> 日最近的工作日
                </Radio>
                <Radio value={6}>本月最后一天</Radio>
                <Radio value={7}>
                    指定{' '}
                    <Select
                        size="small"
                        mode="multiple"
                        style={{minWidth: 220}}
                        placeholder="可多选"
                        value={list}
                        onChange={(v) => setList(v as number[])}
                        options={Array.from({length: 31}, (_, i) => ({label: String(i + 1), value: i + 1}))}
                    />
                </Radio>
            </Radio.Group>
        </div>
    );
}

/** "周" 标签页：1-7 + L/# */
function WeekTab({cron, set}: TabProps) {
    const field = cron.week;
    const init = parseField(field, 'week');
    const [radio, setRadio] = useState(
        field === '?' ? 1 : field === '*' ? 1 : field.includes('#') ? 4 : field.includes('L') ? 5 : init.radio
    );
    const [cycle, setCycle] = useState<[number, number]>(init.cycle as [number, number]);
    const [nth, setNth] = useState<[number, number]>(field.includes('#') ? (field.split('#').map(Number) as [number, number]) : [1, 1]);
    const [last, setLast] = useState<number>(field.includes('L') && field !== 'L' ? Number(field.replace('L', '')) || 7 : 7);
    const [list, setList] = useState<number[]>(init.list);
    const weekLabels = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日'];

    useEffect(() => {
        if (radio === 1) {
            set({week: '?', day: '*'});
        } else if (radio === 2) {
            set({week: `${cycle[0]}-${cycle[1]}`});
        } else if (radio === 3) {
            set({week: list.length ? list.join(',') : '?'});
        } else if (radio === 4) {
            set({week: `${nth[0]}#${nth[1]}`});
        } else if (radio === 5) {
            set({week: `${last === 7 ? 0 : last}L`});
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [radio, cycle[0], cycle[1], list.join(','), nth[0], nth[1], last]);

    return (
        <div style={{padding: '8px 0'}}>
            <Radio.Group
                value={radio}
                onChange={(e) => setRadio(e.target.value)}
                style={{display: 'flex', flexDirection: 'column', gap: 10}}
            >
                <Radio value={1}>不指定（与"日"互斥，此时日必须指定）</Radio>
                <Radio value={2}>
                    周期 周{' '}
                    <Select
                        size="small"
                        value={cycle[0]}
                        onChange={(v) => setCycle([v as number, cycle[1]])}
                        style={{width: 80}}
                        options={weekLabels.slice(1).map((l, i) => ({label: l, value: i + 1}))}
                    />{' '}
                    - 周{' '}
                    <Select
                        size="small"
                        value={cycle[1]}
                        onChange={(v) => setCycle([cycle[0], v as number])}
                        style={{width: 80}}
                        options={weekLabels.slice(1).map((l, i) => ({label: l, value: i + 1}))}
                    />
                </Radio>
                <Radio value={3}>
                    指定{' '}
                    <Checkbox.Group
                        value={list}
                        onChange={(v) => setList(v as number[])}
                        options={weekLabels.slice(1).map((l, i) => ({label: l, value: i + 1}))}
                    />
                </Radio>
                <Radio value={4}>
                    第{' '}
                    <Select
                        size="small"
                        value={nth[0]}
                        onChange={(v) => setNth([v as number, nth[1]])}
                        style={{width: 80}}
                        options={[1, 2, 3, 4].map((n) => ({label: `第${['一', '二', '三', '四'][n - 1]}个`, value: n}))}
                    />{' '}
                    周的周{' '}
                    <Select
                        size="small"
                        value={nth[1]}
                        onChange={(v) => setNth([nth[0], v as number])}
                        style={{width: 80}}
                        options={weekLabels.slice(1).map((l, i) => ({label: l, value: i + 1}))}
                    />
                </Radio>
                <Radio value={5}>
                    本月最后一个{' '}
                    <Select
                        size="small"
                        value={last}
                        onChange={(v) => setLast(v as number)}
                        style={{width: 100}}
                        options={weekLabels.slice(1).map((l, i) => ({label: l, value: i + 1}))}
                    />
                </Radio>
            </Radio.Group>
        </div>
    );
}

/** 主组件 */
export default function CronGenerator({value, onChange, open, onOk, onCancel, title, hideTabs}: CronGeneratorProps) {
    const [cron, setCron] = useState<CronObj>(() => splitCron(value || ''));

    useEffect(() => {
        if (open) setCron(splitCron(value || ''));
    }, [open, value]);

    const set = (patch: Partial<CronObj>) => setCron((prev) => ({...prev, ...patch}));

    const cronString = useMemo(() => joinCron(cron), [cron]);

    useEffect(() => {
        onChange?.(cronString);
    }, [cronString, onChange]);

    const runs = useMemo(() => nextRuns(cronString, 5), [cronString]);

    const tabs: Array<{key: string; label: string; node: React.ReactNode}> = [
        {key: 'second', label: '秒', node: <NumberTab cron={cron} set={set} kind="second" max={59} />},
        {key: 'min', label: '分钟', node: <NumberTab cron={cron} set={set} kind="min" max={59} />},
        {key: 'hour', label: '小时', node: <NumberTab cron={cron} set={set} kind="hour" max={23} />},
        {key: 'day', label: '日', node: <DayTab cron={cron} set={set} />},
        {key: 'month', label: '月', node: <MonthTab cron={cron} set={set} />},
        {key: 'week', label: '周', node: <WeekTab cron={cron} set={set} />},
        {key: 'year', label: '年', node: <YearTab cron={cron} set={set} />}
    ];

    const visibleTabs = tabs.filter((t) => !(hideTabs ?? []).includes(t.key as 'second' | 'min' | 'hour' | 'day' | 'month' | 'week' | 'year'));

    const tabTitles = ['秒', '分钟', '小时', '日', '月', '周', '年'];
    const fieldOrder: Array<keyof CronObj> = ['second', 'min', 'hour', 'day', 'month', 'week', 'year'];

    return (
        <Modal
            open={open}
            onCancel={onCancel}
            onOk={() => onOk?.(cronString)}
            title={
                <span>
                    <ClockCircleOutlined /> {title ?? 'Cron表达式生成器'}
                </span>
            }
            width={760}
            destroyOnClose
            okText="确定"
            cancelText="取消"
        >
            <Tabs items={visibleTabs.map((t) => ({key: t.key, label: t.label, children: t.node}))} />
            <div
                style={{
                    marginTop: 12,
                    padding: '12px 16px',
                    border: '1px solid #d9d9d9',
                    borderRadius: 4,
                    background: '#fafafa'
                }}
            >
                <div style={{marginBottom: 8, fontWeight: 500}}>时间表达式</div>
                <div style={{display: 'flex', justifyContent: 'space-between', textAlign: 'center'}}>
                    {tabTitles.map((t, i) => (
                        <div key={t} style={{flex: 1, padding: '0 4px'}}>
                            <div style={{color: '#999', fontSize: 12, marginBottom: 4}}>{t}</div>
                            <div
                                style={{
                                    background: '#fff',
                                    border: '1px solid #e8e8e8',
                                    borderRadius: 2,
                                    padding: '6px 4px',
                                    fontFamily: 'monospace',
                                    fontSize: 13
                                }}
                            >
                                {cron[fieldOrder[i]!] || '*'}
                            </div>
                        </div>
                    ))}
                    <div style={{flex: 1.5, padding: '0 4px'}}>
                        <div style={{color: '#999', fontSize: 12, marginBottom: 4}}>Cron 表达式</div>
                        <div
                            style={{
                                background: '#fff',
                                border: '1px solid #e8e8e8',
                                borderRadius: 2,
                                padding: '6px 4px',
                                fontFamily: 'monospace',
                                fontSize: 13,
                                wordBreak: 'break-all'
                            }}
                        >
                            {cronString}
                        </div>
                    </div>
                </div>
                <div style={{marginTop: 16, display: 'flex', alignItems: 'flex-start', gap: 8}}>
                    <div style={{flex: 1}}>
                        <div style={{marginBottom: 6, fontWeight: 500}}>最近 5 次运行时间</div>
                        {runs.length === 0 ? (
                            <div style={{color: '#999'}}>计算结果中…</div>
                        ) : (
                            <ul style={{paddingLeft: 20, margin: 0, color: '#555', lineHeight: '22px'}}>
                                {runs.map((t) => (
                                    <li key={t} style={{fontFamily: 'monospace'}}>{t}</li>
                                ))}
                            </ul>
                        )}
                    </div>
                    <Tooltip title="重置">
                        <Button
                            icon={<RedoOutlined />}
                            onClick={() => setCron({...DEFAULT_CRON})}
                        />
                    </Tooltip>
                </div>
            </div>
        </Modal>
    );
}
