import { describe, it, expect } from 'vitest';

import { render, screen, fireEvent, waitFor } from '@testing-library/react'

import { Item } from './index'
import { Story } from '../../feature/StoryFinder';

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


describe('Item Component', () => {

    it('render all properties', () => {
        render(<Item item={storyOne} onRemoveItem={() => { }} />);
        screen.debug();
        expect(screen.getByText('Jordan Walke')).toBeInTheDocument();
        expect(screen.getByText('React')).toHaveAttribute(
            'href',
            'https://reactjs.org/'
        );
    })


})