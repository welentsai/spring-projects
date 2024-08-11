import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'

import axios from 'axios';
import { Story, StoryFinder } from './index';

vi.mock('axios');
// Explicitly type the mocked axios
const mockedAxios = axios as jest.Mocked<typeof axios>;

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

const stories = [storyOne, storyTwo];

describe('Story Finder', () => {

    it('succeeds fetching data', async () => {
        const promise = Promise.resolve({
            data: {
                hits: stories
            }
        });

        mockedAxios.get.mockImplementationOnce(() => promise);

        render(<StoryFinder />)

        expect(screen.queryByText(/Loading/)).toBeInTheDocument();

        await waitFor(async () => await promise);

        expect(screen.queryByText(/Loading/)).toBeNull();
        expect(screen.getByText('React')).toBeInTheDocument();
        expect(screen.getByText('Redux')).toBeInTheDocument();
        expect(screen.getAllByText('Dismiss').length).toBe(2);
        // screen.debug();
    })

    it('fails fetching data', async () => {
        const promise = Promise.reject();

        mockedAxios.get.mockImplementationOnce(() => promise);

        render(<StoryFinder />);

        expect(screen.getByText(/Loading/)).toBeInTheDocument();

        try {
            await waitFor(async () => await promise);
        } catch (error) {
            // screen.debug();
            expect(screen.queryByText(/Loading/)).toBeNull();
            expect(screen.queryByText(/went wrong/)).toBeInTheDocument();
        }
    });

    it('removes a story', async () => {
        const promise = Promise.resolve({
            data: {
                hits: stories,
            },
        });

        mockedAxios.get.mockImplementationOnce(() => promise);

        render(<StoryFinder />);

        await waitFor(async () => await promise);

        expect(screen.getAllByText('Dismiss').length).toBe(2);
        expect(screen.getByText('Jordan Walke')).toBeInTheDocument();

        fireEvent.click(screen.getAllByText('Dismiss')[0]);

        expect(screen.getAllByText('Dismiss').length).toBe(1);
        expect(screen.queryByText('Jordan Walke')).toBeNull();
    });

    it('fails fetching data', async () => {
        const promise = Promise.reject();
        mockedAxios.get.mockImplementationOnce(() => promise);

        render(<StoryFinder />);

        expect(screen.getByText(/Loading/)).toBeInTheDocument();

        try {
            await waitFor(async () => await promise);
        } catch (error) {
            expect(screen.queryByText(/Loading/)).toBeNull();
            expect(screen.queryByText(/went wrong/)).toBeInTheDocument();
        }
    })

    it('remove a story', async () => {
        const promise = Promise.resolve({
            data: {
                hits: stories
            }
        })

        mockedAxios.get.mockImplementationOnce(() => promise);

        render(<StoryFinder />);

        await waitFor(async () => await promise);

        expect(screen.getAllByText('Dismiss').length).toBe(2);
        expect(screen.getByText('Jordan Walke')).toBeInTheDocument();

        fireEvent.click(screen.getAllByText('Dismiss')[0]);

        expect(screen.getAllByText('Dismiss').length).toBe(1);
        expect(screen.queryByText('Jordan Walke')).toBeNull();
    })

    it('searches for specific stories', async () => {
        const reactPromise = Promise.resolve({
            data: {
                hits: stories,
            },
        });

        const anotherStory: Story = {
            title: 'JavaScript',
            url: 'https://en.wikipedia.org/wiki/JavaScript',
            author: 'Brendan Eich',
            num_comments: 15,
            points: 10,
            objectID: "3",
        };

        const javascriptPromise = Promise.resolve({
            data: {
                hits: [anotherStory],
            },
        });

        mockedAxios.get.mockImplementation((url) => {
            if (url.includes('React')) {
                return reactPromise;
            }

            if (url.includes('JavaScript')) {
                return javascriptPromise;
            }

            throw Error();
        });

        // Initial Render
        render(<StoryFinder />);

        expect(screen.queryByText(/Loading/)).toBeInTheDocument();
        // First Data Fetching
        await waitFor(async () => await reactPromise);
        expect(screen.queryByText(/Loading/)).toBeNull();
        expect(screen.getAllByText('Dismiss').length).toBe(2);

        // it will suggest all roles
        // screen.getByRole('');

        expect(screen.queryByDisplayValue('React')).toBeInTheDocument();
        expect(screen.queryByDisplayValue('JavaScript')).toBeNull();

        expect(screen.queryByText('Jordan Walke')).toBeInTheDocument();
        expect(screen.queryByText('Dan Abramov, Andrew Clark')).toBeInTheDocument();
        expect(screen.queryByText('Brendan Eich')).toBeNull();

        // User Interaction -> Search

        // approach 1
        fireEvent.change(screen.getByRole('textbox', { name: /Search:/i }), { target: { value: 'JavaScript' } });

        // approach 2
        // const inputElement = screen.queryByDisplayValue('React');
        // if (inputElement) {
        //     fireEvent.change(inputElement, { target: { value: 'JavaScript' } });
        // } else {
        //     expect.fail();
        // }

        expect(screen.queryByDisplayValue('React')).toBeNull();
        expect(screen.queryByDisplayValue('JavaScript')).toBeInTheDocument();

        // screen.getByRole(""); // list all roles
        fireEvent.click(screen.getByRole('button', { name: /submit/i }));

        // Second Data Fetching
        await waitFor(async () => await javascriptPromise);

        expect(screen.queryByText('Jordan Walke')).toBeNull();
        expect(screen.queryByText('Dan Abramov, Andrew Clark')).toBeNull();
        expect(screen.queryByText('Brendan Eich')).toBeInTheDocument();
    });
});