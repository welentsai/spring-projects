export type Story = {
    objectID: string;
    url: string;
    title: string;
    author: string;
    num_comments: number;
    points: number;
};

export type StoriesState = {
    data: Story[];
    page: number;
    isLoading: boolean;
    isError: boolean;
}

export type StoryPayload = {
    list: Story[];
    page: number;
}

export type StoriesFetchInitAction = {
    type: 'STORIES_FETCH_INIT';
};

export type StoriesFetchSuccessAction = {
    type: 'STORIES_FETCH_SUCCESS';
    payload: {
        list: Story[];
        page: number;
    };
};

export type StoriesFetchFailureAction = {
    type: 'STORIES_FETCH_FAILURE';
};

export type StoriesRemoveAction = {
    type: 'REMOVE_STORY';
    payload: Story;
};

export type StoriesAction =
    | StoriesFetchInitAction
    | StoriesFetchSuccessAction
    | StoriesFetchFailureAction
    | StoriesRemoveAction;