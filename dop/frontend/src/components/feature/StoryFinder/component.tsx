import * as React from 'react';
import axios from 'axios';

import { ThemeProvider } from '../../../context/ThemeContext';
import { SearchForm } from '../SearchForm';
import { List } from '../../common/List';
import { Story } from './type';
import { storiesReducer, useStorageState } from './hooks';
import { API_ENDPOINT } from './constants';

// const getAsyncStories = (): Promise<{ data: { stories: Story[] } }> =>
//   new Promise((resolve, reject) =>
//     setTimeout(
//       () => resolve({ data: { stories: initialStories } }),
//       // () => { reject("Something went wrong!") },
//       2000
//     )
//   );

const StoryFinder = () => {

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
        setUrl(`${API_ENDPOINT}${searchTerm}`);
        event.preventDefault();
    }

    return (
        <ThemeProvider>
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
        </ThemeProvider>
    );
};

export { StoryFinder };
