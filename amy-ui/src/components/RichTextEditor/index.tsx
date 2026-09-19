import {
    AlignCenterOutlined,
    AlignLeftOutlined,
    AlignRightOutlined,
    BoldOutlined,
    ClearOutlined,
    ItalicOutlined,
    LinkOutlined,
    OrderedListOutlined,
    RedoOutlined,
    StrikethroughOutlined,
    UnderlineOutlined,
    UndoOutlined,
    UnorderedListOutlined
} from '@ant-design/icons';
import {ProFormItem} from '@ant-design/pro-components';
import type {ProFormItemProps} from '@ant-design/pro-components';
import {App, Button, Divider, Input, Space, Tooltip} from 'antd';
import {useEffect, useRef} from 'react';

export interface RichTextEditorProps {
    value?: string;
    onChange?: (value: string) => void;
    placeholder?: string;
    disabled?: boolean;
    style?: React.CSSProperties;
}

const FONT_SIZES = [
    {label: '小号', value: '2'},
    {label: '默认', value: '3'},
    {label: '大号', value: '5'}
];

/**
 * 轻量富文本编辑器（contentEditable + document.execCommand），
 * 提供截图所需的基础工具栏：加粗/斜体/下划线/删除线、字体颜色、背景色、
 * 字号、有序/无序列表、对齐、超链接、清除格式、撤销/重做。
 * 支持 value/onChange，可直接放入 ProFormItem 受控使用。
 */
export function RichTextEditor({value, onChange, placeholder, disabled, style}: RichTextEditorProps) {
    const editorRef = useRef<HTMLDivElement>(null);
    const {modal} = App.useApp();

    useEffect(() => {
        const el = editorRef.current;
        // 避免光标位置被打断：仅在外部值与当前内容不一致时回写
        if (el && el.innerHTML !== (value || '')) {
            el.innerHTML = value || '';
        }
    }, [value]);

    const emit = () => onChange?.(editorRef.current?.innerHTML || '');

    const exec = (command: string, arg?: string) => {
        if (disabled) return;
        editorRef.current?.focus();
        document.execCommand(command, false, arg);
        emit();
    };

    const addLink = () => {
        let url = '';
        modal.confirm({
            title: '请输入链接地址',
            content: <Input placeholder="https://" onChange={(e) => {
                url = e.target.value;
            }}/>,
            onOk: () => {
                if (url) exec('createLink', url);
            }
        });
    };

    const toolbar = (
        <Space split={<Divider type="vertical"/>} wrap style={{gap: 4, marginBottom: 8}}>
            <Button size="small" type="text" icon={<BoldOutlined/>} aria-label="加粗" disabled={disabled}
                    onClick={() => exec('bold')}/>
            <Button size="small" type="text" icon={<ItalicOutlined/>} aria-label="斜体" disabled={disabled}
                    onClick={() => exec('italic')}/>
            <Button size="small" type="text" icon={<UnderlineOutlined/>} aria-label="下划线" disabled={disabled}
                    onClick={() => exec('underline')}/>
            <Button size="small" type="text" icon={<StrikethroughOutlined/>} aria-label="删除线" disabled={disabled}
                    onClick={() => exec('strikeThrough')}/>
            <Tooltip title="字体颜色">
                <Input type="color" size="small" disabled={disabled} aria-label="字体颜色"
                       onChange={(e) => exec('foreColor', e.target.value)}
                       style={{width: 32, padding: 0, cursor: 'pointer'}}/>
            </Tooltip>
            <Tooltip title="背景颜色">
                <Input type="color" size="small" disabled={disabled} aria-label="背景颜色"
                       onChange={(e) => exec('hiliteColor', e.target.value)}
                       style={{width: 32, padding: 0, cursor: 'pointer'}}/>
            </Tooltip>
            <select aria-label="字号" defaultValue="3" disabled={disabled}
                    onChange={(e) => exec('fontSize', e.target.value)}
                    style={{height: 24, borderRadius: 4, border: '1px solid #d9d9d9', cursor: 'pointer'}}>
                {FONT_SIZES.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
            </select>
            <Button size="small" type="text" icon={<UnorderedListOutlined/>} aria-label="无序列表" disabled={disabled}
                    onClick={() => exec('insertUnorderedList')}/>
            <Button size="small" type="text" icon={<OrderedListOutlined/>} aria-label="有序列表" disabled={disabled}
                    onClick={() => exec('insertOrderedList')}/>
            <Button size="small" type="text" icon={<AlignLeftOutlined/>} aria-label="左对齐" disabled={disabled}
                    onClick={() => exec('justifyLeft')}/>
            <Button size="small" type="text" icon={<AlignCenterOutlined/>} aria-label="居中对齐" disabled={disabled}
                    onClick={() => exec('justifyCenter')}/>
            <Button size="small" type="text" icon={<AlignRightOutlined/>} aria-label="右对齐" disabled={disabled}
                    onClick={() => exec('justifyRight')}/>
            <Button size="small" type="text" icon={<LinkOutlined/>} aria-label="插入链接" disabled={disabled}
                    onClick={addLink}/>
            <Button size="small" type="text" icon={<ClearOutlined/>} aria-label="清除格式" disabled={disabled}
                    onClick={() => exec('removeFormat')}/>
            <Button size="small" type="text" icon={<UndoOutlined/>} aria-label="撤销" disabled={disabled}
                    onClick={() => exec('undo')}/>
            <Button size="small" type="text" icon={<RedoOutlined/>} aria-label="重做" disabled={disabled}
                    onClick={() => exec('redo')}/>
        </Space>
    );

    return (
        <div style={{border: '1px solid #d9d9d9', borderRadius: 6, padding: 12, background: disabled ? '#f5f5f5' : '#fff', ...style}}>
            {toolbar}
            <div
                ref={editorRef}
                contentEditable={!disabled}
                suppressContentEditableWarning
                data-placeholder={placeholder}
                aria-label="富文本编辑区"
                onInput={emit}
                onBlur={emit}
                style={{
                    minHeight: 160,
                    padding: '4px 8px',
                    outline: 'none',
                    overflowY: 'auto',
                    lineHeight: 1.6,
                    wordBreak: 'break-word'
                }}
            />
        </div>
    );
}

/** ProForm 受控封装，用于 ModalForm / ProForm 内直接声明使用 */
export function ProFormRichText(props: ProFormItemProps<{ value?: string; onChange?: (v: string) => void }>) {
    return <ProFormItem {...props}><RichTextEditor/></ProFormItem>;
}
