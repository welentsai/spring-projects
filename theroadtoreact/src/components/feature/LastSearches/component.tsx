export type LastSearchesProps = {
    lastSearches: string[];
    onLastSearch: (searchTerm: string) => void;
}

export const LastSearches = ({ lastSearches, onLastSearch }: LastSearchesProps) => {
    return (
        <>
            {lastSearches.map((searchTerm, index) => (  // 小括號, 表示為 tsc 語法
                <button
                    key={searchTerm + index}
                    type='button'
                    onClick={() => onLastSearch(searchTerm)}
                >
                    {searchTerm}
                </button>
            ))}
        </>
    );
};
