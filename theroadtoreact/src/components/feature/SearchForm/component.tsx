import { useTheme } from "../../../context/ThemeContext";
import { InputWithLabel } from "../../common/InputWithLabel";

export type SearchFormProps = {
    searchTerm: string;
    handleSearchInput: (event: React.ChangeEvent<HTMLInputElement>) => void;
    handleSearchSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
}

export const SearchForm = ({
    searchTerm,
    handleSearchSubmit,
    handleSearchInput }: SearchFormProps) => {
    const { theme, handleTheme } = useTheme();

    return (
        <form onSubmit={handleSearchSubmit}>
            <InputWithLabel
                id='search'
                value={searchTerm}
                isFocused
                onInputChange={handleSearchInput}
            >
                <strong>Search: -{theme}- </strong>
            </InputWithLabel>
            <button type='submit' disabled={!searchTerm} onClick={() => handleTheme(searchTerm)}>Submit</button>
        </form>
    )
};