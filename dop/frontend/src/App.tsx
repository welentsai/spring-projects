import * as React from 'react';
import axios from 'axios';
import './App.css'
import AppLayout from './components/AppLayout';
import { ThemeContext, useTheme } from './contexts/ThemeContext';
import { theme } from 'antd';

type Story = {
  objectID: string;
  url: string;
  title: string;
  author: string;
  num_comments: number;
  points: number;
};

// const getAsyncStories = (): Promise<{ data: { stories: Story[] } }> =>
//   new Promise((resolve, reject) =>
//     setTimeout(
//       () => resolve({ data: { stories: initialStories } }),
//       // () => { reject("Something went wrong!") },
//       2000
//     )
//   );

type StoriesState = {
  data: Story[];
  isLoading: boolean;
  isError: boolean;
}

type StoriesFetchInitAction = {
  type: 'STORIES_FETCH_INIT';
};

type StoriesFetchSuccessAction = {
  type: 'STORIES_FETCH_SUCCESS';
  payload: Story[];
};

type StoriesFetchFailureAction = {
  type: 'STORIES_FETCH_FAILURE';
};

type StoriesRemoveAction = {
  type: 'REMOVE_STORY';
  payload: Story;
};

type StoriesAction =
  | StoriesFetchInitAction
  | StoriesFetchSuccessAction
  | StoriesFetchFailureAction
  | StoriesRemoveAction;

// A reducer function always receives a state and an action.
// a reducer always return a new state
// a reducer action always associated with a type and as a best practice with a payload
const storiesReducer = (state: StoriesState, action: StoriesAction) => {
  switch (action.type) {
    case 'STORIES_FETCH_INIT':
      return {
        ...state,
        isLoading: true,
        isError: false
      };
    case 'STORIES_FETCH_SUCCESS':
      return {
        ...state,
        isLoading: false,
        isError: false,
        data: action.payload,
      }
    case 'STORIES_FETCH_FAILURE':
      return {
        ...state,
        isLoading: false,
        isError: true,
      };
    case 'REMOVE_STORY':
      return {
        ...state,
        data: state.data.filter(
          (story) => action.payload.objectID !== story.objectID
        ),
      }
    default:
      throw new Error();
  }
}

const useStorageState = (key: string, initialState: string) => {
  const [value, setValue] = React.useState(
    localStorage.getItem(key) || initialState
  );

  React.useEffect(() => {
    localStorage.setItem(key, value);
  }, [value]);

  return [value, setValue] as const;
};

const API_ENDPOINT = 'https://hn.algolia.com/api/v1/search?query=';

const App: React.FC = () => {

  const [theme, setTheme] = React.useState<string>("green");

  // React custom hooks
  const [searchTerm, setSearchTerm] = useStorageState('search', 'React');

  const [url, setUrl] = React.useState(`${API_ENDPOINT}${searchTerm}`)

  // React reducer (multiple state management)
  const [stories, dispatchStories] = React.useReducer(
    storiesReducer,
    { data: [], isLoading: false, isError: false, }
  );

  const handleFetchStories = React.useCallback(async () => {

    // if searchTerm not present, e.g. null, empty string, undefined
    // if (!searchTerm) return;

    dispatchStories({ type: 'STORIES_FETCH_INIT' });

    try {
      const result = await axios.get(url);
      dispatchStories({
        type: 'STORIES_FETCH_SUCCESS',
        payload: result.data.hits,
      });

    } catch {
      dispatchStories({ type: 'STORIES_FETCH_FAILURE' })
    }

  }, [url]);

  React.useEffect(() => {
    handleFetchStories();
  }, [handleFetchStories]);

  const handleRemoveStory = (item: Story) => {
    dispatchStories({
      type: 'REMOVE_STORY',
      payload: item
    })
  }

  const handleSearchInput = (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    setSearchTerm(event.target.value);
  }

  const handleSearchSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    console.log(`handleSearchSubmit -> ${API_ENDPOINT}${searchTerm}`);
    setUrl(`${API_ENDPOINT}${searchTerm}`)

    event.preventDefault();
  }

  return (
    <ThemeContext.Provider value={{ theme, setTheme }}>
      <div>
        <h1>My Hacker Stories</h1>

        <SearchForm
          searchTerm={searchTerm}
          handleSearchInput={handleSearchInput}
          handleSearchSubmit={handleSearchSubmit}
        />

        <hr />

        {stories.isError && <p>Something went wrong ...</p>}

        {stories.isLoading ? (
          <p>Loading ...</p>
        ) : (
          <List
            list={stories.data}
            onRemoveItem={handleRemoveStory} />
        )}

        <hr />
        <p>{Math.random() * 100}</p>
      </div>
    </ThemeContext.Provider>
  );
  // return <AppLayout />;

};

type SearchFormProps = {
  searchTerm: string;
  handleSearchInput: (event: React.ChangeEvent<HTMLInputElement>) => void;
  handleSearchSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
}

const SearchForm = ({
  searchTerm,
  handleSearchSubmit,
  handleSearchInput }: SearchFormProps) => {
  const { value, onChange } = useTheme();

  return (
    <form onSubmit={handleSearchSubmit}>
      <InputWithLabel
        id='search'
        value={searchTerm}
        isFocused
        onInputChange={handleSearchInput}
      >
        <strong>Search: -{value}- </strong>
      </InputWithLabel>
      <button type='submit' disabled={!searchTerm}>Submit</button>
    </form>
  )
}

type InputProps = {
  id: string;
  isFocused: boolean;
  value: string;
  type?: string;
  children: React.ReactNode;
  onInputChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
}

const InputWithLabel = ({
  id,
  isFocused,
  value,
  type = 'text',
  children,
  onInputChange }: InputProps) => {

  const inputRef = React.useRef<HTMLInputElement>(null);

  React.useEffect(() => {
    if (isFocused && inputRef.current) {
      inputRef.current.focus();
    }
  }, [isFocused]);

  return (
    <>
      <label htmlFor={id}>{children}</label>
      &nbsp;
      <input
        ref={inputRef}
        id={id}
        type={type}
        value={value}
        onChange={onInputChange}
      />
    </>
  )
}

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

export default App
