export function buildTree<T extends {
    children?: T[]
}>(items: T[], idKey: keyof T, parentKey: keyof T, rootValue = 0): T[] {
    const itemMap = new Map<unknown, T>();
    const roots: T[] = [];
    items.forEach((item) => itemMap.set(item[idKey], {...item, children: []} as T));
    itemMap.forEach((item) => {
        const parentId = item[parentKey];
        const parent = itemMap.get(parentId);
        if (parent && parentId !== rootValue) parent.children?.push(item);
        else roots.push(item);
    });
    return roots;
}
