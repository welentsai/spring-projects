import * as React from 'react';
import { useStoryApi } from '../StoryFetcher/storyFetcherReducer';
import { StoriesState, Story } from '../StoryFinder';
import { API_ENDPOINT } from '../StoryFinder/constants';
import { List } from '../../common/List';

const initialState: StoriesState = {
    data: [],
    isLoading: true,
    isError: false,
    page: 0,
}

const api = `${API_ENDPOINT}react`

const StoryBoard = () => {
    const [state, handleUrl, handleRemoveStory] = useStoryApi(api, initialState);

    React.useEffect(() => {

    }, [])

    return (
        <div>
            <h1>My Hacker Stories</h1>

            <List
                list={state.data}
                onRemoveItem={handleRemoveStory} />

            {state.isLoading ? (
                <p>Loading ...</p>
            ) : (
                <div>
                    <button type='button'>
                        More
                    </button>
                    <hr />
                </div>

            )}
        </div>
    )
}

export { StoryBoard } 