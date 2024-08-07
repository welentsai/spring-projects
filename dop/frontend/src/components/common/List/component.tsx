import { Story } from "../../feature/StoryFinder/type";

type ListProps = {
    list: Story[];
    onRemoveItem: (item: Story) => void;
};

const List = ({ list, onRemoveItem }: ListProps) => (
    <ul>
        {list.map((item) => (
            <Item key={item.objectID} item={item} onRemoveItem={onRemoveItem} />
        ))}
    </ul>
)

type ItemProps = {
    item: Story;
    onRemoveItem: (item: Story) => void;
};

// destructuring the props object right away in the component’s function signature.
const Item = ({ item, onRemoveItem }: ItemProps) => (
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

export { List };