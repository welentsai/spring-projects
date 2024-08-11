import { describe, it, expect } from 'vitest';

import { storiesReducer, StoriesRemoveAction, StoriesState, Story } from './index';

const storyOne: Story = {
    title: 'React',
    url: 'https://reactjs.org/',
    author: 'Jordan Walke',
    num_comments: 3,
    points: 4,
    objectID: '0'
}

const storyTwo: Story = {
    title: 'Redux',
    url: 'https://redux.js.org/',
    author: 'Dan Abramov, Andrew Clark',
    num_comments: 2,
    points: 5,
    objectID: '1'
}

const storyNotExist: Story = {
    title: 'Vitest',
    url: 'https://vitest.org',
    author: 'Donno',
    num_comments: 2,
    points: 5,
    objectID: '3'
}



const stories = [storyOne, storyTwo];


describe('storiesReducer', () => {
    it('remove a story from all stories, should remove target story successfully', () => {
        const action: StoriesRemoveAction = {
            type: 'REMOVE_STORY',
            payload: storyOne
        }

        const state: StoriesState = {
            data: stories,
            isLoading: false,
            isError: false
        }

        const newState = storiesReducer(state, action);

        const expectedState: StoriesState = {
            data: [storyTwo],
            isLoading: false,
            isError: false
        };

        expect(newState).toStrictEqual(expectedState);
    });

    it('remove a not exist story from all stories, should nothing change', () => {
        const action: StoriesRemoveAction = {
            type: 'REMOVE_STORY',
            payload: storyNotExist
        }

        const state: StoriesState = {
            data: stories,
            isLoading: false,
            isError: false
        }

        const newState = storiesReducer(state, action);

        expect(newState).toStrictEqual(state);
    });
})