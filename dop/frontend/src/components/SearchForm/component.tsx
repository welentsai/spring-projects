import { useTheme } from "../../contexts/ThemeContext";
import { InputWithLabel } from "../InputWithLabel";

type SearchFormProps = {
    searchTerm: string;
    handleSearchInput: (event: React.ChangeEvent<HTMLInputElement>) => void;
    handleSearchSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
}

const SearchForm = ({
    searchTerm,
    handleSearchSubmit,
    handleSearchInput }: SearchFormProps) => {
    const { value } = useTheme();

    return (
        <form onSubmit={handleSearchSubmit}>
            <InputWithLabel
                id='search'
                value={searchTerm}
                isFocused
                onInputChange={handleSearchInput}
            >
                <strong>Search: -{value}- </strong>
            </InputWithLabel>
            <button type='submit' disabled={!searchTerm}>Submit</button>
        </form>
    )
}

export { SearchForm };