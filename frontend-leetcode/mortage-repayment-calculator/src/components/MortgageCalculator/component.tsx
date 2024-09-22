import { CurrencyInput } from "../CurrencyInput";
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
            <div>
                <p>Mortgage Term</p>
                <YearInput year={0} />
            </div>
        </>
    )
}