/**
 * Cron 表达式工具
 *
 * 参考 ruoyi-ui 的 components/Crontab/result.vue：把 6~7 位的 cron 串解析为
 * 秒/分/时/日/月/周/年 七个数组，再从当前时间往后推最近 N 次执行时间。
 *
 * 这是 amy-ui 端本地实现，不依赖任何第三方 cron 库；与 Quartz 兼容的 6/7
 * 位表达式（minute 精度及以上）均能正确处理 W / L / # / ? 等扩展字符。
 */

export interface CronArrays {
    second: number[];
    minute: number[];
    hour: number[];
    day: number[];
    month: number[];
    year: number[];
    /** 解析过程中识别出的"日"特殊规则，影响预览循环 */
    dayRule: '' | 'lastDay' | 'workDay' | 'weekDay' | 'assWeek' | 'lastWeek';
    dayRuleSup: number[] | number | string;
    /** 解析过程中识别出的"周"规则集合（仅在 dayRule 触发时填充） */
    weekUsed: number[];
}

/** 把 N-M 周期规则展开为数字数组 */
function getCycleArr(rule: string, limit: number, fromZero: boolean): number[] {
    const [aRaw, bRaw] = rule.split('-').map(Number);
    let min = aRaw ?? 0;
    let max = bRaw ?? 0;
    if (min > max) max += limit;
    const arr: number[] = [];
    for (let i = min; i <= max; i++) {
        let add = 0;
        if (!fromZero && i % limit === 0) add = limit;
        arr.push(Math.round((i % limit) + add));
    }
    arr.sort((x, y) => y - x);
    return arr;
}

/** 把 X/Y 平均规则展开为数字数组 */
function getAverageArr(rule: string, limit: number): number[] {
    const [aRaw, bRaw] = rule.split('/').map(Number);
    let min = aRaw ?? 0;
    const step = bRaw ?? 1;
    const arr: number[] = [];
    while (min <= limit) {
        arr.push(min);
        min += step;
    }
    return arr;
}

/** 把 1,2,3 这种指定值规则展开为数组 */
function getAssignArr(rule: string): number[] {
    return rule.split(',').map(Number).sort((x, y) => y - x);
}

/** 把 a..b 的整数展开为数组 */
function getOrderArr(min: number, max: number): number[] {
    const arr: number[] = [];
    for (let i = min; i <= max; i++) arr.push(i);
    return arr;
}

/** 把 cron 表达式解析为秒/分/时/日/月/年六个数组 */
export function parseCron(expression: string, baseYear: number): CronArrays {
    const parts = (expression || '').trim().split(/\s+/);
    const [sec = '*', min = '*', hour = '*', day = '*', month = '*', week = '?', year = ''] = parts;
    const out: CronArrays = {
        second: [],
        minute: [],
        hour: [],
        day: [],
        month: [],
        year: [],
        dayRule: '',
        dayRuleSup: '',
        weekUsed: []
    };

    // 秒
    out.second = getOrderArr(0, 59);
    if (sec.includes('-')) out.second = getCycleArr(sec, 60, true);
    else if (sec.includes('/')) out.second = getAverageArr(sec, 59);
    else if (sec !== '*') out.second = getAssignArr(sec);

    // 分
    out.minute = getOrderArr(0, 59);
    if (min.includes('-')) out.minute = getCycleArr(min, 60, true);
    else if (min.includes('/')) out.minute = getAverageArr(min, 59);
    else if (min !== '*') out.minute = getAssignArr(min);

    // 时
    out.hour = getOrderArr(0, 23);
    if (hour.includes('-')) out.hour = getCycleArr(hour, 24, true);
    else if (hour.includes('/')) out.hour = getAverageArr(hour, 23);
    else if (hour !== '*') out.hour = getAssignArr(hour);

    // 日（带 W / L 等特殊规则）
    out.day = getOrderArr(1, 31);
    out.dayRule = '';
    out.dayRuleSup = '';
    if (day.includes('-')) {
        out.day = getCycleArr(day, 31, false);
        out.dayRuleSup = 'null';
    } else if (day.includes('/')) {
        out.day = getAverageArr(day, 31);
        out.dayRuleSup = 'null';
    } else if (day.includes('W')) {
        out.dayRule = 'workDay';
        const m = day.match(/(\d{1,2})?W/);
        out.dayRuleSup = m && m[1] ? Number(m[1]) : 1;
        out.day = [out.dayRuleSup as number];
    } else if (day.includes('L')) {
        out.dayRule = 'lastDay';
        out.dayRuleSup = 'null';
        out.day = [31];
    } else if (day !== '*' && day !== '?') {
        out.day = getAssignArr(day);
        out.dayRuleSup = 'null';
    } else if (day === '*') {
        out.dayRuleSup = 'null';
    }

    // 月
    out.month = getOrderArr(1, 12);
    if (month.includes('-')) out.month = getCycleArr(month, 12, false);
    else if (month.includes('/')) out.month = getAverageArr(month, 12);
    else if (month !== '*') out.month = getAssignArr(month);

    // 周
    if (out.dayRule === '') {
        if (week.includes('-')) {
            out.dayRule = 'weekDay';
            out.dayRuleSup = getCycleArr(week, 7, false);
        } else if (week.includes('#')) {
            out.dayRule = 'assWeek';
            const m = week.match(/(\d)#(\d)/);
            out.dayRuleSup = m ? [Number(m[1]), Number(m[2])] : [1, 1];
            out.day = [1];
        } else if (week.includes('L')) {
            out.dayRule = 'lastWeek';
            const m = week.match(/(\d{1,2})?L/);
            let v = m && m[1] ? Number(m[1]) : 7;
            if (v === 7) v = 0;
            out.dayRuleSup = v;
            out.day = [31];
        } else if (week !== '*' && week !== '?') {
            out.dayRule = 'weekDay';
            out.dayRuleSup = getAssignArr(week);
        }
    }

    // 年
    out.year = getOrderArr(baseYear, baseYear + 100);
    if (year && year !== '*') {
        if (year.includes('-')) out.year = getCycleArr(year, baseYear + 100, false);
        else if (year.includes('/')) out.year = getAverageArr(year, baseYear + 100);
        else out.year = getAssignArr(year);
    }

    return out;
}

/** 数组中的索引（值 <= 给定值 时归 0） */
function getIndex(arr: number[], value: number): number {
    const first = arr[0]!;
    const last = arr[arr.length - 1]!;
    if (value <= first || value > last) return 0;
    for (let i = 0; i < arr.length - 1; i++) {
        const cur = arr[i]!;
        const next = arr[i + 1]!;
        if (value > cur && value <= next) return i + 1;
    }
    return 0;
}

/** 校验日期是否合法 */
function checkDate(value: string): boolean {
    const time = new Date(value);
    if (isNaN(time.getTime())) return false;
    const fmt =
        time.getFullYear() +
        '-' +
        String(time.getMonth() + 1).padStart(2, '0') +
        '-' +
        String(time.getDate()).padStart(2, '0') +
        ' ' +
        String(time.getHours()).padStart(2, '0') +
        ':' +
        String(time.getMinutes()).padStart(2, '0') +
        ':' +
        String(time.getSeconds()).padStart(2, '0');
    return value === fmt;
}

/** 推断日期是星期几（quartz：1=周日, 7=周六） */
function dayOfWeek(value: string): number {
    return new Date(value).getDay() + 1;
}

/**
 * 计算给定 cron 表达式接下来 N 次执行时间。
 * 失败（无匹配 / 解析异常）时返回空数组。
 */
export function nextRuns(expression: string, count = 5): string[] {
    if (!expression || !expression.trim()) return [];
    try {
        const now = new Date();
        const baseYear = now.getFullYear();
        const arr = parseCron(expression, baseYear);
        const sDate = arr.second;
        const mDate = arr.minute;
        const hDate = arr.hour;
        const DDate = arr.day;
        const MDate = arr.month;
        const YDate = arr.year;
        let nYear = now.getFullYear();
        let nMonth = now.getMonth() + 1;
        let nDay = now.getDate();
        let nHour = now.getHours();
        let nMin = now.getMinutes();
        let nSecond = now.getSeconds();

        let sIdx = getIndex(sDate, nSecond);
        let mIdx = getIndex(mDate, nMin);
        let hIdx = getIndex(hDate, nHour);
        let DIdx = getIndex(DDate, nDay);
        let MIdx = getIndex(MDate, nMonth);
        const YIdx = getIndex(YDate, nYear);

        const reset = {
            second: () => {
                sIdx = 0;
                nSecond = sDate[0]!;
            },
            minute: () => {
                mIdx = 0;
                nMin = mDate[0]!;
                reset.second();
            },
            hour: () => {
                hIdx = 0;
                nHour = hDate[0]!;
                reset.minute();
            },
            day: () => {
                DIdx = 0;
                nDay = DDate[0]!;
                reset.hour();
            },
            month: () => {
                MIdx = 0;
                nMonth = MDate[0]!;
                reset.day();
            }
        };

        const result: string[] = [];
        const firstMonth = MDate[MIdx]!;
        const firstDay = DDate[DIdx]!;
        if (nYear !== YDate[YIdx]) reset.month();
        if (nMonth !== MDate[MIdx]) reset.day();
        if (nDay !== DDate[DIdx]) reset.hour();
        if (nHour !== hDate[hIdx]) reset.minute();
        if (nMin !== mDate[mIdx]) reset.second();

        /** 当迭代到"未来日期"时，把时分秒计数器全部归零，否则从当前时间起算会跳过后续匹配 */
        const resetIfFutureDay = (Di: number, Mi: number) => {
            const futureMonth = Mi > MIdx || firstMonth !== MDate[MIdx]!;
            const futureDay = Di > DIdx || futureMonth;
            if (futureDay) {
                nHour = hDate[0]!;
                hIdx = 0;
                nMin = mDate[0]!;
                mIdx = 0;
                nSecond = sDate[0]!;
                sIdx = 0;
            }
        };

        outer: for (let Yi = YIdx; Yi < YDate.length; Yi++) {
            const YY = YDate[Yi]!;
            if (nMonth > MDate[MDate.length - 1]!) {
                reset.month();
                continue;
            }
            for (let Mi = MIdx; Mi < MDate.length; Mi++) {
                const MM = String(MDate[Mi]!).padStart(2, '0');
                if (nDay > DDate[DDate.length - 1]!) {
                    reset.day();
                    if (Mi === MDate.length - 1) {
                        reset.month();
                        continue outer;
                    }
                    continue;
                }
                for (let Di = DIdx; Di < DDate.length; Di++) {
                    resetIfFutureDay(Di, Mi);
                    let DD = DDate[Di]!;
                    let thisDD = String(DD).padStart(2, '0');
                    if (nHour > hDate[hDate.length - 1]!) {
                        reset.hour();
                        if (Di === DDate.length - 1) {
                            reset.day();
                            if (Mi === MDate.length - 1) {
                                reset.month();
                                continue outer;
                            }
                            continue;
                        }
                        continue;
                    }
                    if (
                        !checkDate(`${YY}-${MM}-${thisDD} 00:00:00`) &&
                        arr.dayRule !== 'workDay' &&
                        arr.dayRule !== 'lastWeek' &&
                        arr.dayRule !== 'lastDay'
                    ) {
                        reset.day();
                        continue;
                    }
                    if (arr.dayRule === 'lastDay') {
                        if (!checkDate(`${YY}-${MM}-${thisDD} 00:00:00`)) {
                            while (DD > 0 && !checkDate(`${YY}-${MM}-${String(DD).padStart(2, '0')} 00:00:00`)) DD--;
                            thisDD = String(DD).padStart(2, '0');
                        }
                    } else if (arr.dayRule === 'workDay') {
                        const dayRuleSup = arr.dayRuleSup as number;
                        if (!checkDate(`${YY}-${MM}-${thisDD} 00:00:00`)) {
                            while (DD > 0 && !checkDate(`${YY}-${MM}-${String(DD).padStart(2, '0')} 00:00:00`)) DD--;
                            thisDD = String(DD).padStart(2, '0');
                        }
                        const w = dayOfWeek(`${YY}-${MM}-${thisDD} 00:00:00`);
                        if (w === 1) {
                            DD++;
                            thisDD = String(DD).padStart(2, '0');
                            if (!checkDate(`${YY}-${MM}-${thisDD} 00:00:00`)) DD -= 3;
                        } else if (w === 7) {
                            if (dayRuleSup !== 1) DD--;
                            else DD += 2;
                            thisDD = String(DD).padStart(2, '0');
                        }
                    } else if (arr.dayRule === 'weekDay') {
                        const pool = arr.dayRuleSup as number[];
                        const w = dayOfWeek(`${YY}-${MM}-${String(DD).padStart(2, '0')} 00:00:00`);
                        if (!pool.includes(w)) {
                            if (Di === DDate.length - 1) {
                                reset.day();
                                if (Mi === MDate.length - 1) {
                                    reset.month();
                                    continue outer;
                                }
                                continue;
                            }
                            continue;
                        }
                    } else if (arr.dayRule === 'assWeek') {
                        const sup = arr.dayRuleSup as number[];
                        const w = dayOfWeek(`${YY}-${MM}-01 00:00:00`);
                        DD = sup[0]! >= w ? (sup[0]! - 1) * 7 + sup[1]! - w + 1 : sup[0]! * 7 + sup[1]! - w + 1;
                        thisDD = String(DD).padStart(2, '0');
                    } else if (arr.dayRule === 'lastWeek') {
                        const want = arr.dayRuleSup as number;
                        if (!checkDate(`${YY}-${MM}-${thisDD} 00:00:00`)) {
                            while (DD > 0 && !checkDate(`${YY}-${MM}-${String(DD).padStart(2, '0')} 00:00:00`)) DD--;
                            thisDD = String(DD).padStart(2, '0');
                        }
                        const w = dayOfWeek(`${YY}-${MM}-${thisDD} 00:00:00`);
                        if (want < w) DD -= w - want;
                        else if (want > w) DD -= 7 - (want - w);
                        thisDD = String(DD).padStart(2, '0');
                    }
                    DD = Number(thisDD);
                    for (let hi = hIdx; hi < hDate.length; hi++) {
                        const hh = String(hDate[hi]!).padStart(2, '0');
                        if (nMin > mDate[mDate.length - 1]!) {
                            reset.minute();
                            if (hi === hDate.length - 1) {
                                reset.hour();
                                if (Di === DDate.length - 1) {
                                    reset.day();
                                    if (Mi === MDate.length - 1) {
                                        reset.month();
                                        continue outer;
                                    }
                                    continue;
                                }
                                continue;
                            }
                            continue;
                        }
                        for (let mi = mIdx; mi < mDate.length; mi++) {
                            const mm = String(mDate[mi]!).padStart(2, '0');
                            if (nSecond > sDate[sDate.length - 1]!) {
                                reset.second();
                                if (mi === mDate.length - 1) {
                                    reset.minute();
                                    if (hi === hDate.length - 1) {
                                        reset.hour();
                                        if (Di === DDate.length - 1) {
                                            reset.day();
                                            if (Mi === MDate.length - 1) {
                                                reset.month();
                                                continue outer;
                                            }
                                            continue;
                                        }
                                        continue;
                                    }
                                    continue;
                                }
                                continue;
                            }
                            for (let si = sIdx; si <= sDate.length - 1; si++) {
                                const ss = String(sDate[si]!).padStart(2, '0');
                                const fmtMM = String(MDate[Mi]!).padStart(2, '0');
                                const fmtDD = String(DD).padStart(2, '0');
                                if (fmtMM !== '00' && fmtDD !== '00') {
                                    result.push(`${YY}-${fmtMM}-${fmtDD} ${hh}:${mm}:${ss}`);
                                }
                                if (result.length >= count) break outer;
                                if (si === sDate.length - 1) {
                                    reset.second();
                                    if (mi === mDate.length - 1) {
                                        reset.minute();
                                        if (hi === hDate.length - 1) {
                                            reset.hour();
                                            if (Di === DDate.length - 1) {
                                                reset.day();
                                                if (Mi === MDate.length - 1) {
                                                    reset.month();
                                                    continue outer;
                                                }
                                                continue;
                                            }
                                            continue;
                                        }
                                        continue;
                                    }
                                    continue;
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    } catch {
        return [];
    }
}
