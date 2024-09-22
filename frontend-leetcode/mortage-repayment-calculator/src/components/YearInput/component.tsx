
export type YearInputPros = {
    year: number;
}

export const YearInput = ({ year }: YearInputPros) => {
    return (
        <div className="flex justify-between overflow-hidden rounded-md border border-gray-300">
            <input
                className="px-2 py-1 w-36"
                type='text'
                value={year}
                placeholder="Enter Year"
            />
            <span className="px-2 py-1 bg-sky-300 font-bold text-sky-600">Year</span>
        </div>
    )
}