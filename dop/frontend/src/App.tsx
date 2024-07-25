import * as React from 'react';
import './App.css'
import AppLayout from './components/AppLayout';

type Story = {
  objectID: number;
  url: string;
  title: string;
  author: string;
  num_comments: number;
  points: number;
};

const initialStories: Story[] = [
  {
    title: 'React',
    url: 'https://reactjs.org/',
    author: 'Jordan Walke',
    num_comments: 3,
    points: 4,
    objectID: 0,
  },
  {
    title: 'Redux',
    url: 'https://redux.js.org/',
    author: 'Dan Abramov, Andrew Clark',
    num_comments: 2,
    points: 5,
    objectID: 1,
  },
];

const useStorageState = (key: string, initialState: string) => {
  const [value, setValue] = React.useState(
    localStorage.getItem(key) || initialState
  );

  React.useEffect(() => {
    localStorage.setItem(key, value);
  }, [value]);

  return [value, setValue] as const;
};

const App: React.FC = () => {
  // const [searchTerm, setSearchTerm] = React.useState(localStorage.getItem('search') || 'React');
  // const [searchTerm, setSearchTerm] = React.useState(localStorage.getItem('search') ?? 'React');  // nullish operator

  // React custom hooks
  const [searchTerm, setSearchTerm] = useStorageState('search', 'React');

  const [stories, setStories] = React.useState<Story[]>(initialStories);

  const handleSearch = (event: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(event.target.value);
  }

  const handleRemoveStory = (item: Story) => {
    const newStories = stories.filter(
      (story) => item.objectID !== story.objectID
    );

    setStories(newStories);
  }

  const searchedStories = stories.filter(story => story.title.toLowerCase().includes(searchTerm.toLowerCase()));

  return (
    <div>
      <h1>My Hacker Stories</h1>
      <InputWithLabel
        id='search'
        isFocused
        value={searchTerm}
        onInputChange={handleSearch}
      >
        <strong>Search: </strong>
      </InputWithLabel>
      {/* <Search search={searchTerm} onSearch={handleSearch} /> */}
      <hr />
      <List list={searchedStories} onRemoveItem={handleRemoveStory} />
    </div>
  );
  // return <AppLayout />;
};

type InputProps = {
  id: string;
  isFocused: boolean;
  value: string;
  type?: string;
  children: React.ReactNode;
  onInputChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
}

const InputWithLabel = ({ id, isFocused, value, type = 'text', children, onInputChange }: InputProps) => (
  <>
    <label htmlFor={id}>{children}</label>
    &nbsp;
    <input
      id={id}
      type={type}
      value={value}
      autoFocus={isFocused}
      onChange={onInputChange}
    />
  </>
)

type ListProps = {
  list: Story[];
  onRemoveItem: (item: any) => void;
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

export default App
