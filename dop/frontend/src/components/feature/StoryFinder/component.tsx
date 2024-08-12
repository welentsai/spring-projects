import * as React from 'react';
import axios from 'axios';

import { ThemeProvider } from '../../../context/ThemeContext';
import { SearchForm } from '../SearchForm';
import { List } from '../../common/List';
import { Story } from './type';
import { storiesReducer, useStorageState } from './hooks';
import { API_BASE, API_SEARCH, PARAM_PAGE, PARAM_SEARCH } from './constants';
import { LastSearches } from '../LastSearches';

// const getAsyncStories = (): Promise<{ data: { stories: Story[] } }> =>
//   new Promise((resolve, reject) =>
//     setTimeout(
//       () => resolve({ data: { stories: initialStories } }),
//       // () => { reject("Something went wrong!") },
//       2000
//     )
//   );

const extractSearchTerm = (url: string) =>
    url
        .substring(url.lastIndexOf('?') + 1, url.lastIndexOf('&'))
        .replace(PARAM_SEARCH, '');

const getLastSearches = (urls: string[]) =>
    urls
        .reduce((result, url, index) => {
            const searchTerm = extractSearchTerm(url);

            if (index === 0) {
                return result.concat(searchTerm);
            }

            const previousSearchTerms = result[result.length - 1];

            if (searchTerm === previousSearchTerms) {
                return result;
            } else {
                return result.concat(searchTerm);
            }

        }, [] as string[])
        .slice(-6)
        .slice(0, -1)
        .map(extractSearchTerm);

const getUrl = (searchTerm: string, page: number) =>
    `${API_BASE}${API_SEARCH}?${PARAM_SEARCH}${searchTerm}&${PARAM_PAGE}${page}`;

const StoryFinder = () => {

    // React custom hooks
    const [searchTerm, setSearchTerm] = useStorageState('search', 'React');

    const [urls, setUrls] = React.useState<string[]>([getUrl(searchTerm, 0)]);

    // React reducer (multiple state management)
    const [stories, dispatchStories] = React.useReducer(
        storiesReducer,
        { data: [], page: 0, isLoading: false, isError: false, }
    );


    const handleSearch = (searchTerm: string, page: number) => {
        const url = getUrl(searchTerm, page);
        setUrls(urls.concat(url));
    }

    const handleLastSearch = (searchTerm: string) => {
        setSearchTerm(searchTerm);
        handleSearch(searchTerm, 0);
    }

    const lastSearches = getLastSearches(urls);

    const handleFetchStories = React.useCallback(async () => {

        dispatchStories({ type: 'STORIES_FETCH_INIT' });

        try {
            const lastUrl = urls[urls.length - 1];

            const result = await axios.get(lastUrl);
            dispatchStories({
                type: 'STORIES_FETCH_SUCCESS',
                payload: {
                    list: result.data.hits,
                    page: result.data.page
                },
            });

        } catch {
            dispatchStories({ type: 'STORIES_FETCH_FAILURE' })
        }

    }, [urls]);

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
        handleSearch(searchTerm, 0);
        event.preventDefault();
    }

    const handleMore = () => {
        const lastUrl = urls[urls.length - 1];
        const searchTerm = extractSearchTerm(lastUrl);
        handleSearch(searchTerm, stories.page + 1);
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

                <LastSearches
                    lastSearches={lastSearches}
                    onLastSearch={handleLastSearch}
                />

                <hr />

                {stories.isError && <p>Something went wrong ...</p>}

                <List
                    list={stories.data}
                    onRemoveItem={handleRemoveStory} />

                {stories.isLoading ? (
                    <p>Loading ...</p>
                ) : (
                    <button type='button' onClick={handleMore}>
                        More
                    </button>
                )}

                <hr />
                <p>{Math.random() * 100}</p>
            </div>
        </ThemeProvider >
    );
};

export { StoryFinder };
