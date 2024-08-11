import { describe, it, expect, vi } from 'vitest';

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
        expect(screen.getByRole('button')).toBeInTheDocument();
    })

    it('render a clickable dismiss button', () => {
        render(<Item item={storyOne} onRemoveItem={() => { }} />);

        //screen.getByRole('');  // neat feature (it suggests roles if provided role not available) 

        expect(screen.getByRole('button')).toBeInTheDocument();
    })

    it('clicking the dismiss button calls the callback handler', () => {
        const handleRemoveItem = vi.fn();

        render(<Item item={storyOne} onRemoveItem={handleRemoveItem} />);

        fireEvent.click(screen.getByRole('button'));

        expect(handleRemoveItem).toHaveBeenCalledTimes(1);
    })


})