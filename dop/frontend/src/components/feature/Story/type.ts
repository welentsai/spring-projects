type Story = {
    objectID: string;
    url: string;
    title: string;
    author: string;
    num_comments: number;
    points: number;
};

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

export type { Story, StoriesState, StoriesAction };