import * as React from 'react';
import axios from 'axios';
import { StoriesAction, StoriesState, Story } from '../StoryFinder';


const storyReducer = (state: StoriesState, action: StoriesAction): StoriesState => {
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
                data: action.payload.list,
                page: action.payload.page,
                isLoading: false,
                isError: false
            };
        case 'STORIES_FETCH_FAILURE':
            return { ...state };
        case 'REMOVE_STORY':
            return { ...state };
    }
}

export const useStoryApi = (initialUrl: string, initialState: StoriesState):
    [StoriesState, (url: string) => void] => {

    const [url, setUrl] = React.useState<string>(initialUrl);
    const [state, dispatch] = React.useReducer(storyReducer, initialState);

    React.useEffect(() => {
        let didCancel = false;

        const fetchData = async () => {
            dispatch({ type: 'STORIES_FETCH_INIT' })

            try {
                const result = await axios.get(url);
                if (didCancel) {
                    console.log(result);
                    dispatch({
                        type: 'STORIES_FETCH_SUCCESS',
                        payload: {
                            list: result.data.hits,
                            page: result.data.page
                        },
                    });
                }
            } catch (err) {
                dispatch({ type: 'STORIES_FETCH_FAILURE' });
            }
        }

        fetchData();

        return () => {
            didCancel = true;
        }
    }, [url]);


    return [state, setUrl];
}