import {UploadOutlined} from '@ant-design/icons';
import {Button, Image, Typography, Upload} from 'antd';
import {useState} from 'react';
import {uploadImage} from '@/services/sunpalaceartspace/project';

interface CoverUploadProps {
    value?: string;
    onChange?: (url: string) => void;
}

/**
 * 封面图上传（200*200），上传成功后回填 URL。
 * 支持 value/onChange，可直接放入 ProFormItem 受控使用。
 * 布局：左侧预览，右侧竖向排「上传按钮 + 尺寸提示」，避免窄列里右侧留出大片空白。
 */
export function CoverUpload({value, onChange}: CoverUploadProps) {
    const [uploading, setUploading] = useState(false);

    const handleUpload = async (file: File) => {
        setUploading(true);
        try {
            const res = await uploadImage(file);
            onChange?.(res.data?.url || '');
        } finally {
            setUploading(false);
        }
    };

    return (
        <div style={{display: 'flex', gap: 12, alignItems: 'flex-start'}}>
            {value
                ? <Image width={100} height={100} src={value} alt="封面图预览"
                         style={{objectFit: 'cover', borderRadius: 6, border: '1px solid #f0f0f0', flex: 'none'}}/>
                : <div style={{
                    width: 100,
                    height: 100,
                    flex: 'none',
                    borderRadius: 6,
                    border: '1px dashed #d9d9d9',
                    background: '#fafafa',
                    color: '#bfbfbf',
                    fontSize: 12,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                }}>暂无封面</div>}
            <div style={{flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', gap: 8}}>
                <Upload
                    accept="image/*"
                    showUploadList={false}
                    maxCount={1}
                    beforeUpload={(file) => {
                        void handleUpload(file);
                        return false;
                    }}
                >
                    <Button icon={<UploadOutlined/>} loading={uploading} block>
                        {value ? '重新上传' : '上传图片'}
                    </Button>
                </Upload>
                <Typography.Text type="secondary" style={{fontSize: 12}}>建议尺寸 200*200</Typography.Text>
            </div>
        </div>
    );
}
