import {request} from '@umijs/max';

export async function downloadFile(url: string, data: Record<string, unknown>, filename: string) {
    const blob = await request<Blob>(url, {
        method: 'POST',
        data,
        responseType: 'blob'
    });
    const objectUrl = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = objectUrl;
    anchor.download = filename;
    anchor.click();
    URL.revokeObjectURL(objectUrl);
}
