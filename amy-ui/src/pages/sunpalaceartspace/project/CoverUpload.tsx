import {UploadOutlined} from '@ant-design/icons';
import {Button, Image, Space, Typography, Upload} from 'antd';
import {useState} from 'react';
import {uploadImage} from '@/services/sunpalaceartspace/project';

interface CoverUploadProps {
    value?: string;
    onChange?: (url: string) => void;
}

/**
 * 封面图上传（200*200），上传成功后回填 URL。
 * 支持 value/onChange，可直接放入 ProFormItem 受控使用。
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
        <Space align="start" size={16} wrap>
            <Space direction="vertical" size={8}>
                <Upload
                    accept="image/*"
                    showUploadList={false}
                    maxCount={1}
                    beforeUpload={(file) => {
                        void handleUpload(file);
                        return false;
                    }}
                >
                    <Button icon={<UploadOutlined/>} loading={uploading}>上传图片</Button>
                </Upload>
                <Typography.Text type="secondary">建议尺寸 200*200</Typography.Text>
            </Space>
            {value ? <Image width={100} height={100} src={value} alt="封面图预览"
                            style={{objectFit: 'cover', borderRadius: 4, border: '1px solid #f0f0f0'}}/> : null}
        </Space>
    );
}
