import { useState } from 'react'
import './App.css'
import { CenterLayout } from './layout/CenterLayout';
import { MortgageCalculator } from './components/MortgageCalculator';
import { MortgageResult } from './components/MortgageResult';

function App() {
  const [amount, setAmount] = useState(500)

  function handleAmountChange(amount: number): void {
    console.log('new amount', amount);
    setAmount(amount);
  }

  return (
    <>
      <CenterLayout>
        <div className="bg-white rounded-lg shadow-lg overflow-hidden max-w-4xl w-full">
          <div className="flex">
            {/* Left column */}
            <div className="w-1/2 p-4 bg-gray-50">
              <MortgageCalculator
                amount={amount}
                handleAmountChange={handleAmountChange}
              />
            </div>
            {/* Right column */}
            <div className="w-1/2  p-4 bg-blue-800 text-white">
              <MortgageResult
                monthlyRepayment={0}
                totalRepayment={0}
              />
            </div>
          </div>
        </div>
      </CenterLayout>
    </>
  )
}

export default App
