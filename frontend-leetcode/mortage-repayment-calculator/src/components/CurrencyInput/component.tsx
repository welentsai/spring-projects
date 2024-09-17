import * as React from 'react';

export type CurrencyInputProps = {
    amount: number,
    handleAmountChange: (amount: number) => void
}

function parseValue(str: string): number {
    const numericValue = str.replace(/,/g, '');
    return parseFloat(numericValue) || 0;
}

function formatValue(num: number): string {
    return num.toLocaleString('en-GB', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

export const CurrencyInput = ({ amount, handleAmountChange }: CurrencyInputProps) => {

    const [displayValue, setDisplayValue] = React.useState(formatValue(amount));

    function handleInputChange(event: React.ChangeEvent<HTMLInputElement>): void {
        const inputValue = event.target.value;
        setDisplayValue(inputValue);

        // Only update the actual numeric value if the input is valid
        if (/^[£]?\d{1,3}(,\d{3})*(\.\d{0,2})?$/.test(inputValue.replace(/[£]/g, ''))) {
            const numericValue = parseValue(inputValue);
            handleAmountChange(numericValue);
        }
    }

    function handleBlur(): void {
        const numericValue = parseValue(displayValue);
        setDisplayValue(formatValue(numericValue));
        handleAmountChange(numericValue);
    }

    return (
        <div className="flex items-stretch max-w-md overflow-hidden rounded-md border border-gray-300 focus-within:ring-2 focus-within:ring-blue-500 focus-within:border-blue-500">
            <div className="flex items-center justify-center bg-sky-300 px-3">
                <span className="text-white font-semibold">£</span>
            </div>
            <input
                type="text"
                value={displayValue}
                onChange={handleInputChange}
                onBlur={handleBlur}
                className="flex-grow py-2 px-3 text-gray-700 bg-white focus:outline-none"
                placeholder="Enter amount"
            />
        </div>
    );
}