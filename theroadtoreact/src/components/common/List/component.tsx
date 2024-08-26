import * as React from 'react';
import { Story } from "../../feature/StoryFinder/type";
import { sortBy } from 'lodash';

// dictionary for all sorting
const SORTS = {
    NONE: (list: Story[]) => list,
    TITLE: (list: Story[]) => sortBy(list, 'title'),
    AUTHOR: (list: Story[]) => sortBy(list, 'author'),
    COMMENTS: (list: Story[]) => sortBy(list, 'num_comments').reverse(),
    POINTS: (list: Story[]) => sortBy(list, 'points').reverse(),
}

export type ListProps = {
    list: Story[];
    onRemoveItem: (item: Story) => void;
};

// define the type for the keys of SORTS
type SortKey = keyof typeof SORTS;

type Sort = {
    sortKey: SortKey,
    isReverse: boolean
}

export const List = ({ list, onRemoveItem }: ListProps) => {



    // Make sure your 'sort' variable is of this type
    const [sort, setSort] = React.useState<Sort>({
        sortKey: 'NONE',
        isReverse: false
    });

    const handleSort = (sortKey: SortKey) => {

        // click twice to toggle reverse
        const isReverse = sortKey === sort.sortKey && !sort.isReverse;

        setSort({
            sortKey,
            isReverse
        });
    }

    const sortFunction = SORTS[sort.sortKey];
    const sortedList = sort.isReverse
        ? sortFunction(list).reverse()
        : sortFunction(list);

    return (
        <ul>
            <li style={{ display: 'flex' }}>
                <span style={{ width: '40%' }}>
                    <button type='button' onClick={() => handleSort('TITLE')}>Title</button>
                </span>
                <span style={{ width: '30%' }}>
                    <button type='button' onClick={() => handleSort('AUTHOR')}>Author</button>
                </span>
                <span style={{ width: '10%' }}>
                    <button type='button' onClick={() => handleSort('COMMENTS')}>Comments</button>
                </span>
                <span style={{ width: '10%' }}>
                    <button type='button' onClick={() => handleSort('POINTS')}>Points</button></span>
                <span style={{ width: '10%' }}>Actions</span>
            </li>
            {
                sortedList.map((item) => (
                    <Item key={item.objectID} item={item} onRemoveItem={onRemoveItem} />
                ))
            }
        </ul >
    )
}

export type ItemProps = {
    item: Story;
    onRemoveItem: (item: Story) => void;
};

// destructuring the props object right away in the component’s function signature.
export const Item = ({ item, onRemoveItem }: ItemProps) => (
    <li style={{ display: 'flex' }}>
        <span style={{ width: '40%' }}>
            <a href={item.url}>{item.title}</a>
        </span>
        <span style={{ width: '30%' }}>{item.author}</span>
        <span style={{ width: '10%' }}>{item.num_comments}</span>
        <span style={{ width: '10%' }}>{item.points}</span>
        <span style={{ width: '10%' }}>
            {/* onClick  inline handler */}
            <button type='button' onClick={() => onRemoveItem(item)}>Dismiss</button>
        </span>
    </li>
);