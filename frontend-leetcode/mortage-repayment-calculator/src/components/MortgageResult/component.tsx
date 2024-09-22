
export type MortgageResultProps = {
    monthlyRepayment: number;
    totalRepayment: number;
}

export const MortgageResult = ({ monthlyRepayment, totalRepayment }: MortgageResultProps) => {
    return (
        <>
            <h3 className="text-xl font-semibold mb-4">Your results</h3>
            <p className="text-sm mb-6">
                Your results are shown below based on the information you provided. To adjust the results, edit the form and click "calculate repayments" again.
            </p>
            <div className="bg-gray-700 p-4 rounded-lg">
                <p className="text-sm mb-2">Your monthly repayments</p>
                <p className="text-3xl font-bold text-yellow-400">£{monthlyRepayment.toFixed(2)}</p>
                <p className="text-sm mt-4 mb-2">Total you'll repay over the term</p>
                <p className="text-xl font-semibold">£{totalRepayment.toFixed(2)}</p>
            </div>
        </>
    )
}