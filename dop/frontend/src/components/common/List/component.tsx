import { Story } from "../../feature/StoryFinder/type";

export type ListProps = {
    list: Story[];
    onRemoveItem: (item: Story) => void;
};

export const List = ({ list, onRemoveItem }: ListProps) => (
    <ul>
        {list.map((item) => (
            <Item key={item.objectID} item={item} onRemoveItem={onRemoveItem} />
        ))}
    </ul>
)

export type ItemProps = {
    item: Story;
    onRemoveItem: (item: Story) => void;
};

// destructuring the props object right away in the component’s function signature.
export const Item = ({ item, onRemoveItem }: ItemProps) => (
    <li>
        <span>
            <a href={item.url}>{item.title}</a>
        </span>
        <span>{item.author}</span>
        <span>{item.num_comments}</span>
        <span>{item.points}</span>
        <span>
            {/* onClick  inline handler */}
            <button type='button' onClick={() => onRemoveItem(item)}>Dismiss</button>
        </span>

    </li>
);