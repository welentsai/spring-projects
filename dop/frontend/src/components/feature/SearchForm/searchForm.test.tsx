import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { SearchForm, SearchFormProps } from './index';

describe('SearchForm', () => {

    const searchFormProps: SearchFormProps = {
        searchTerm: 'React',
        handleSearchSubmit: vi.fn(),
        handleSearchInput: vi.fn()
    }

    it('renders the input field with its value', () => {
        render(<SearchForm {...searchFormProps} />);
        screen.debug();
        expect(screen.getByDisplayValue('React')).toBeInTheDocument();
    })

    it('renders the correct label', () => {
        render(<SearchForm {...searchFormProps} />);

        expect(screen.getByLabelText(/Search/)).toBeInTheDocument();
    })

    it('calls onSearchInput on input field change', () => {
        render(<SearchForm {...searchFormProps} />);
        fireEvent.change(screen.getByDisplayValue('React'), { target: { value: 'Redeux' } });

        expect(searchFormProps.handleSearchInput).toHaveBeenCalledTimes(1);
    })

    it('calls onSearchSubmit on button submit', () => {
        render(<SearchForm {...searchFormProps} />);

        fireEvent.submit(screen.getByRole('button'));

        expect(searchFormProps.handleSearchSubmit).toHaveBeenCalledTimes(1);
    })
});


