import { CurrencyInput } from "../CurrencyInput";
import { InterestInput } from "../InterestInput";
import { YearInput } from "../YearInput";

export type MortgageCalculatorProps = {
    amount: number;
    handleAmountChange: (amount: number) => void
}

export const MortgageCalculator = ({ amount, handleAmountChange }: MortgageCalculatorProps) => {
    return (
        <>
            <div className='flex justify-between'>
                <h2 className='text-xl'>Mortgage Calculator</h2>
                <p className='p-2'>Clear all</p>
            </div>

            <div className=''>
                <p>Mortgage Account</p>
                <CurrencyInput amount={amount} handleAmountChange={handleAmountChange} />
            </div>

            <div className='flex'>
                <div className="w-1/2 pr-2">
                    <p>Mortgage Term</p>
                    <YearInput
                        year={0}
                    />
                </div>
                <div className="w-1/2 pl-2">
                    <p>Interest Rate</p>
                    <InterestInput
                        interest={5}
                    />
                </div>
            </div>

        </>
    )
}