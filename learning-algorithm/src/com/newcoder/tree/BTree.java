package com.newcoder.tree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
定义：
	1.  节点中元素升序排序
	2.  每个叶节点有相同的高度
	3.  节点有 n 个元素，至多有 n+1 个子节点
	4.  每个节点最少 t-1 个元素，最多 2t-1个元素( t 由程序员指定)
	5. 根节点至少2个子节点
	
搜索：按照多叉树搜索即可

插入：
	1.  找到应该插入的叶节点位置
	2.  判断该节点是否已满
	3.  未满：直接插入
	4.  满了：分裂，中间值上升为父节点，递归向上依次处理
	5.  最后，将元素插入对应叶节点

删除（删除节点u中的关键字k）：
	case1：u是叶节点，直接删（递归的终止条件）
	case2：
		1. k的左孩子u1的关键字至少t个，则从左孩子中找到最大的k'代替k，递归删除k'
		2. k的右孩子u2的关键字至少t个，则从右孩子中找到最大的k'代替k，递归删除k'
		3. 如果1，2都不成立，说明左孩子和右孩子都是t-1，将k和u2合并后并入u1，递归删除k
	case3（查找关键字位置的过程中，u.pi是包含k的子树）：
		1. 若u.pi中至少包含t个关键字，继续扫描（无法合并）
		2. 若u.pi中只包含t-1个关键字
			1）如果u.pi，至少有一个相邻的兄弟丰满（至少t个关键字）：
				例如左兄弟节点比较丰满，将u.pi对应的u的位置的左边的关键字下降至u.pi最左边，将做兄弟节点的最右边上升至u.pi对对应u的位置的左边
			2）如果u.pi的两个兄弟都不丰满：合并u.pi和其中一个兄弟，再将u的一个关键字下降至新合并的节点，作为中间节点，继续扫描
 */
public class BTree<K, V> {
    /**
     *@Desc B树节点中的键值对。
     */
    private static class Entry<K, V> {
        private K key;
        private V value;

        public Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() { return key; }
        public V getValue() { return value; }
        public void setValue(V value) { this.value = value; }
        @Override
        public String toString() { return key + ":" + value; }
    }

    /**
     *@Desc 在B树节点中搜索给定键值的返回结果。
     */
    private static class SearchResult<V> {
        private boolean exist;	// 此次查找是否成功
        private int index;		// 给定键值在B树节点中的位置（如果查找失败，则表示子节点应该在哪个子节点中）
        private V value;		

        public SearchResult(boolean exist, int index) {
            this.exist = exist;
            this.index = index;
        }

        public SearchResult(boolean exist, int index, V value) {
            this(exist, index);
            this.value = value;
        }

        public boolean isExist() { return exist; }
        public int getIndex() { return index; }
        public V getValue() { return value; }
    }

    /**
     *@Desc B树中的节点。
     */
    private static class BTreeNode<K, V> {
        /** 节点的项，按键非降序存放 */
        private volatile List<Entry<K,V>> entrys;
        /** 内节点的子节点 */
        private volatile List<BTreeNode<K, V>> children;
        /** 是否为叶子节点 */
        private boolean leaf;
        /** 键的比较函数对象 */
        private Comparator<K> kComparator;

        private BTreeNode() {
            entrys = new ArrayList<Entry<K, V>>();
            children = new ArrayList<BTreeNode<K, V>>();
            leaf = false;
        }

        public BTreeNode(Comparator<K> kComparator) {
            this();
            this.kComparator = kComparator;
        }

        public boolean isLeaf() { return leaf; }
        public void setLeaf(boolean leaf) { this.leaf = leaf; }

        /**
         * @Desc 关键字的个数
         */
        public int size() {
            return entrys.size();
        }

        /**
         * @Desc key值比较
         */
        @SuppressWarnings("unchecked")
		public int compare(K key1, K key2) {
            return kComparator == null ? ((Comparable<K>)key1).compareTo(key2) : kComparator.compare(key1, key2);
        }

        /**
         *@Desc 二分查找指定key
         */
        public SearchResult<V> searchKey(K key) {
            int low = 0;
            int high = entrys.size() - 1;
            int mid = 0;
            while(low <= high)
            {
                mid = (low + high) / 2;
                Entry<K, V> entry = entrys.get(mid);
                if(compare(entry.getKey(), key) == 0)
                    break;
                else if(compare(entry.getKey(), key) > 0)
                    high = mid - 1;
                else
                    low = mid + 1;
            }
            boolean result = false;
            int index = 0;
            V value = null;
            if(low <= high) { // 查找成功
                result = true;
                index = mid; // index表示元素所在的位置
                value = entrys.get(index).getValue();
            }
            else {
                result = false;
                index = low; // index表示元素应该存在的位置
            }
            return new SearchResult<V>(result, index, value);
        }

        /**
         *@Desc 将给定的项追加到节点的末尾
         */
        public void addEntry(Entry<K, V> entry) {
            entrys.add(entry);
        }

        /**
         *@Desc 删除给定索引的entry
         */
        public Entry<K, V> removeEntry(int index) {
            return entrys.remove(index);
        }

        /**
         *@Desc 得到节点中给定索引的项。
         */
        public Entry<K, V> entryAt(int index) {
            return entrys.get(index);
        }

        /**
         *@Desc 插入entry
         * 	如果节点中存在给定的键，则更新其关联的值。
         * 	否则插入。
         */
        public V putEntry(Entry<K, V> entry) {
            SearchResult<V> result = searchKey(entry.getKey());
            if(result.isExist()) {
                V oldValue = entrys.get(result.getIndex()).getValue();
                entrys.get(result.getIndex()).setValue(entry.getValue());
                return oldValue;
            } else {
                insertEntry(entry, result.getIndex());
                return null;
            }
        }

        /**
         *@Desc 在该节点中插入给定的项
         */
        public boolean insertEntry(Entry<K, V> entry)
        {
            SearchResult<V> result = searchKey(entry.getKey());
            if(result.isExist()) {
                return false;
            } else {
                insertEntry(entry, result.getIndex());
                return true;
            }
        }

        /**
         *@Desc 在该节点中给定索引的位置插入给定的项
         */
        public void insertEntry(Entry<K, V> entry, int index) {
            List<Entry<K, V>> newEntrys = new ArrayList<Entry<K, V>>();
            int i = 0;
            // index = 0或者index = keys.size()都没有问题
            for(; i < index; ++ i)
                newEntrys.add(entrys.get(i));
            newEntrys.add(entry);
            for(; i < entrys.size(); ++ i)
                newEntrys.add(entrys.get(i));
            entrys.clear();
            entrys = newEntrys;
        }

        /**
         *@Desc 返回节点中给定索引的子节点。
         */
        public BTreeNode<K, V> childAt(int index) {
            if(isLeaf())
                throw new UnsupportedOperationException("Leaf node doesn't have children.");
            return children.get(index);
        }

        /**
         *@Desc 将给定的子节点追加到该节点的末尾
         */
        public void addChild(BTreeNode<K, V> child) {
            children.add(child);
        }

        /**
         *@Desc 删除该节点中给定索引位置的子节点 
         */
        public void removeChild(int index) {
            children.remove(index);
        }

        /**
         *@Desc 将给定的子节点插入到该节点中给定索引的位置。
         */
        public void insertChild(BTreeNode<K, V> child, int index)
        {
            List<BTreeNode<K, V>> newChildren = new ArrayList<BTreeNode<K, V>>();
            int i = 0;
            for(; i < index; ++ i)
                newChildren.add(children.get(i));
            newChildren.add(child);
            for(; i < children.size(); ++ i)
                newChildren.add(children.get(i));
            children = newChildren;
        }
    }

    private static final int DEFAULT_T = 2;

    /** B树的根节点 */
    private BTreeNode<K, V> root;
    /** 根据B树的定义，B树的每个非根节点的关键字数n满足(t - 1) <= n <= (2t - 1) */
    private int t = DEFAULT_T;
    /** 非根节点中最小的键值数 */
    private int minKeySize = t - 1;
    /** 非根节点中最大的键值数 */
    private int maxKeySize = 2*t - 1;
    /** 键的比较函数对象 */
    private Comparator<K> kComparator;

    /**
     *@Desc 构造一颗B树，键值采用采用自然排序方式
     */
    public BTree() {
        root = new BTreeNode<K, V>();
        root.setLeaf(true);
    }

    public BTree(int t) {
        this();
        this.t = t;
        minKeySize = t - 1;
        maxKeySize = 2*t - 1;
    }

    /**
     *@Desc 以给定的键值比较函数对象构造一颗B树。
     */
    public BTree(Comparator<K> kComparator) {
        root = new BTreeNode<K, V>(kComparator);
        root.setLeaf(true);
        this.kComparator = kComparator;
    }

    public BTree(Comparator<K> kComparator, int t) {
        this(kComparator);
        this.t = t;
        minKeySize = t - 1;
        maxKeySize = 2*t - 1;
    }

    @SuppressWarnings("unchecked")
	int compare(K key1, K key2)
    {
        return kComparator == null ? ((Comparable<K>)key1).compareTo(key2) : kComparator.compare(key1, key2);
    }

    /**
     *@Desc 搜索给定的键 
     */
    public V search(K key)
    {
        return search(root, key);
    }

    /**
     *@Desc 在以给定节点为根的子树中，递归搜索给定的<code>key</code>
     */
    private V search(BTreeNode<K, V> node, K key)
    {
        SearchResult<V> result = node.searchKey(key);
        if(result.isExist()) {
            return result.getValue();
        } else {
            if(node.isLeaf())
                return null;
            else
                search(node.childAt(result.getIndex()), key);
        }
        return null;
    }

    /**
     *@Desc 分裂一个满子节点<code>childNode</code>。
     *	将中间节点(t-1)拉出来放进父节点中
     *	(t-1)右侧独立出来成为新子节点（ps：它的孩子也要移交给新子节点）
     */
    private void splitNode(BTreeNode<K, V> parentNode, BTreeNode<K, V> childNode, int index) {
        assert childNode.size() == maxKeySize;

        BTreeNode<K, V> siblingNode = new BTreeNode<K, V>(kComparator);
        siblingNode.setLeaf(childNode.isLeaf());
        // 将满子节点中索引为[t, 2t - 2]的(t - 1)个项插入新的节点中
        for(int i = 0; i < minKeySize; ++ i)
            siblingNode.addEntry(childNode.entryAt(t + i));
        // 提取满子节点中的中间项，其索引为(t - 1)
        Entry<K, V> entry = childNode.entryAt(t - 1);
        // 删除满子节点中索引为[t - 1, 2t - 2]的t个项
        for(int i = maxKeySize - 1; i >= t - 1; -- i)
            childNode.removeEntry(i);
        if(!childNode.isLeaf()) { // 如果满子节点不是叶节点，则还需要处理其子节点
            // 将满子节点中索引为[t, 2t - 1]的t个子节点插入新的节点中
            for(int i = 0; i < minKeySize + 1; ++ i)
                siblingNode.addChild(childNode.childAt(t + i));
            // 删除满子节点中索引为[t, 2t - 1]的t个子节点
            for(int i = maxKeySize; i >= t; -- i)
                childNode.removeChild(i);
        }
        // 将entry插入父节点
        parentNode.insertEntry(entry, index);
        // 将新节点插入父节点
        parentNode.insertChild(siblingNode, index + 1);
    }

    /**
     *@Desc 在一个非满节点中插入给定的项。
     */
    private boolean insertNotFull(BTreeNode<K, V> node, Entry<K, V> entry) {
        assert node.size() < maxKeySize;

        if(node.isLeaf()) {
        	// 如果是叶子节点，直接插入
            return node.insertEntry(entry);
        } else {
            // 找到entry在给定节点应该插入的位置，那么entry应该插入该位置对应的子树中
            SearchResult<V> result = node.searchKey(entry.getKey());
            // 如果存在，则直接返回失败
            if(result.isExist())
                return false;
            
            BTreeNode<K, V> childNode = node.childAt(result.getIndex());
            if(childNode.size() == 2*t - 1) {
                // 如果子节点是满节点, 则先分裂
                splitNode(node, childNode, result.getIndex());
                // 如果给定entry的键大于分裂之后新生成项的键，则需要插入该新项的右边， 否则左边。
                if(compare(entry.getKey(), node.entryAt(result.getIndex()).getKey()) > 0)
                    childNode = node.childAt(result.getIndex() + 1);
            }
            return insertNotFull(childNode, entry);
        }
    }

    /**
     *@Desc 在B树中插入给定的键值对。
     */
    public boolean insert(K key, V value) {
        if(root.size() == maxKeySize) { // 如果根节点满了，则B树长高
            BTreeNode<K, V> newRoot = new BTreeNode<K, V>(kComparator);
            newRoot.setLeaf(false);
            newRoot.addChild(root);
            splitNode(newRoot, root, 0);
            root = newRoot;
        }
        return insertNotFull(root, new Entry<K, V>(key, value));
    }

    /**
     *@Desc 如果存在给定的键，则更新键关联的值，否则插入给定的项。(同insertNotFull)
     */
    private V putNotFull(BTreeNode<K, V> node, Entry<K, V> entry)
    {
        assert node.size() < maxKeySize;

        if(node.isLeaf()) {
        	// 如果是叶子节点，直接插入
            return node.putEntry(entry);
        } else {
            // 找到entry在给定节点应该插入的位置，那么entry应该插入该位置对应的子树中
            SearchResult<V> result = node.searchKey(entry.getKey());
            // 如果存在，则更新
            if(result.isExist())
                return node.putEntry(entry);
            
            BTreeNode<K, V> childNode = node.childAt(result.getIndex());
            if(childNode.size() == 2*t - 1) {
                // 如果子节点是满节点, 则先分裂
                splitNode(node, childNode, result.getIndex());
                // 如果给定entry的键大于分裂之后新生成项的键，则需要插入该新项的右边，否则左边。
                if(compare(entry.getKey(), node.entryAt(result.getIndex()).getKey()) > 0)
                    childNode = node.childAt(result.getIndex() + 1);
            }
            return putNotFull(childNode, entry);
        }
    }

    /**
     *@Desc 如果B树中存在给定的键，则更新值，否则插入。
     */
    public V put(K key, V value) {
        if(root.size() == maxKeySize) {// 如果根节点满了，则B树长高
            BTreeNode<K, V> newRoot = new BTreeNode<K, V>(kComparator);
            newRoot.setLeaf(false);
            newRoot.addChild(root);
            splitNode(newRoot, root, 0);
            root = newRoot;
        }
        return putNotFull(root, new Entry<K, V>(key, value));
    }

    /**
     *@Desc 从B树中删除一个与给定键关联的项 
     */
    public Entry<K, V> delete(K key) {
        return delete(root, key);
    }

    /**
     *@Desc 从以给定<code>node</code>为根的子树中删除与给定键关联的项。
     */
    private Entry<K, V> delete(BTreeNode<K, V> node, K key) {
        // 该过程需要保证，对非根节点执行删除操作时，其关键字个数至少为t。
        assert node.size() >= t || node == root;

        SearchResult<V> result = node.searchKey(key);
        
        // 因为这是查找成功的情况，0 <= result.getIndex() <= (node.size() - 1)，因此(result.getIndex() + 1)不会溢出
        if(result.isExist()) {
            // 1.如果关键字在节点node中，并且是叶节点，则直接删除。
            if(node.isLeaf()) {
                return node.removeEntry(result.getIndex());
            } else {
                // 2.a 如果节点node.key的左孩子至少包含至少t个项，则从左孩子中找到最大的代替此节点，删除之（递归至叶节点）
                BTreeNode<K, V> leftChildNode = node.childAt(result.getIndex());
                if(leftChildNode.size() >= t) {
                    // 使用leftChildNode中的最后一个项代替node中需要删除的项
                    node.removeEntry(result.getIndex());
                    node.insertEntry(leftChildNode.entryAt(leftChildNode.size() - 1), result.getIndex());
                    // 递归删除左子节点中的最后一个项
                    return delete(leftChildNode, leftChildNode.entryAt(leftChildNode.size() - 1).getKey());
                }
                else {
                    // 2.b 如果节点node.key的右孩子至少包含t个项，则从右孩子中找到最小的代替此节点，删除之（递归至叶节点）
                    BTreeNode<K, V> rightChildNode = node.childAt(result.getIndex() + 1);
                    if(rightChildNode.size() >= t) {
                        // 使用rightChildNode中的第一个项代替node中需要删除的项
                        node.removeEntry(result.getIndex());
                        node.insertEntry(rightChildNode.entryAt(0), result.getIndex());
                        // 递归删除右子节点中的第一个项
                        return delete(rightChildNode, rightChildNode.entryAt(0).getKey());
                    } else { 
                    	// 2.c node.key的左孩子和右孩子都是t-1个，将右孩子和node.key一起并入左孩子，递归删除
                        Entry<K, V> deletedEntry = node.removeEntry(result.getIndex());
                        node.removeChild(result.getIndex() + 1);
                        // 将node中与key关联的项和rightChildNode中的项合并进leftChildNode
                        leftChildNode.addEntry(deletedEntry);
                        for(int i = 0; i < rightChildNode.size(); ++ i)
                            leftChildNode.addEntry(rightChildNode.entryAt(i));
                        // 将rightChildNode中的子节点合并进leftChildNode，如果有的话
                        if(!rightChildNode.isLeaf()) {
                            for(int i = 0; i <= rightChildNode.size(); ++ i)
                                leftChildNode.addChild(rightChildNode.childAt(i));
                        }
                        return delete(leftChildNode, key);
                    }
                }
            }
        } else {
            // 查找过程中，node.index是包含key的子树
            if(node.isLeaf()) {
            	// 如果关键字不在节点node中，并且是叶节点，则什么都不做，因为该关键字不在该B树中
                System.out.println("The key: " + key + " isn't in this BTree.");
                return null;
            }
            
            BTreeNode<K, V> childNode = node.childAt(result.getIndex());
            if(childNode.size() >= t) { // 如果子节点有不少于t个项，无法合并，则递归删除
                return delete(childNode, key);
            } else {
            	// 该子树只包含t-1个关键字，需合并
                // 先查找右边的兄弟节点
                BTreeNode<K, V> siblingNode = null;
                int siblingIndex = -1;
                if(result.getIndex() < node.size()) { // 存在右兄弟节点
                    if(node.childAt(result.getIndex() + 1).size() >= t) {
                        siblingNode = node.childAt(result.getIndex() + 1);
                        siblingIndex = result.getIndex() + 1;
                    }
                }
                // 如果右边的兄弟节点不符合条件，则试试左边的兄弟节点
                if(siblingNode == null) {
                    if(result.getIndex() > 0) { // 存在左兄弟节点 
                        if(node.childAt(result.getIndex() - 1).size() >= t) {
                            siblingNode = node.childAt(result.getIndex() - 1);
                            siblingIndex = result.getIndex() - 1;
                        }
                    }
                }
                // 3.a 有一个相邻兄弟节点至少包含t个项（将兄弟的上司移至child里面，将兄弟节点的一个键上升至node）
                if(siblingNode != null) {
                    if(siblingIndex < result.getIndex()) {// 左兄弟节点满足条件
                        childNode.insertEntry(node.entryAt(siblingIndex), 0);
                        node.removeEntry(siblingIndex);
                        node.insertEntry(siblingNode.entryAt(siblingNode.size() - 1), siblingIndex);
                        siblingNode.removeEntry(siblingNode.size() - 1);
                        // 将左兄弟节点的最后一个孩子移到childNode
                        if(!siblingNode.isLeaf()) {
                            childNode.insertChild(siblingNode.childAt(siblingNode.size()), 0);
                            siblingNode.removeChild(siblingNode.size());
                        }
                    } else {// 右兄弟节点满足条件 
                        childNode.insertEntry(node.entryAt(result.getIndex()), childNode.size() - 1);
                        node.removeEntry(result.getIndex());
                        node.insertEntry(siblingNode.entryAt(0), result.getIndex());
                        siblingNode.removeEntry(0);
                        // 将右兄弟节点的第一个孩子移到childNode
                        if(!siblingNode.isLeaf()) {
                            childNode.addChild(siblingNode.childAt(0));
                            siblingNode.removeChild(0);
                        }
                    }
                    return delete(childNode, key);
                } else {// 3.b 如果其相邻左右节点都包含t-1个项：合并child和其中一个兄弟，再将node中的一个键值下降至新合并的节点（成为中间节点）
                    if(result.getIndex() < node.size()) { // 存在右兄弟，直接在后面追加
                        BTreeNode<K, V> rightSiblingNode = node.childAt(result.getIndex() + 1);
                        childNode.addEntry(node.entryAt(result.getIndex()));
                        node.removeEntry(result.getIndex());
                        node.removeChild(result.getIndex() + 1);
                        for(int i = 0; i < rightSiblingNode.size(); ++ i)
                            childNode.addEntry(rightSiblingNode.entryAt(i));
                        if(!rightSiblingNode.isLeaf()) {
                            for(int i = 0; i <= rightSiblingNode.size(); ++ i)
                                childNode.addChild(rightSiblingNode.childAt(i));
                        }
                    } else {// 存在左节点，在前面插入 
                        BTreeNode<K, V> leftSiblingNode = node.childAt(result.getIndex() - 1);
                        childNode.insertEntry(node.entryAt(result.getIndex() - 1), 0);
                        node.removeEntry(result.getIndex() - 1);
                        node.removeChild(result.getIndex() - 1);
                        for(int i = leftSiblingNode.size() - 1; i >= 0; -- i)
                            childNode.insertEntry(leftSiblingNode.entryAt(i), 0);
                        if(!leftSiblingNode.isLeaf()) {
                            for(int i = leftSiblingNode.size(); i >= 0; -- i)
                                childNode.insertChild(leftSiblingNode.childAt(i), 0);
                        }
                    }
                    // 如果node是root并且node不包含任何项了
                    if(node == root && node.size() == 0)
                        root = childNode;
                    return delete(childNode, key);
                }
            }
        }
    }
}