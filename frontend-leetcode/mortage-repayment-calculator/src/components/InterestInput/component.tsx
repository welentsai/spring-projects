
export type InterestInputProps = {
    interest: number
}

export const InterestInput = ({ interest }: InterestInputProps) => {

    return (
        <div className="flex justify-between overflow-hidden rounded-md border border-gray-300">
            <input
                className="px-2 py-1 w-32"
                type='text'
                value={interest}
                placeholder="Enter Interest"
            />
            <span className="px-2 py-1 bg-sky-300 font-bold text-sky-600">Interest</span>
        </div>
    )
}