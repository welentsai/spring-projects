import { useState } from 'react'
import './App.css'
import { CurrencyInput } from './components/CurrencyInput'

function App() {
  const [amount, setAmount] = useState(500)

  function handleAmountChange(amount: number): void {
    console.log('new amount', amount);
    setAmount(amount);
  }

  return (
    <>
      <div className="min-h-screen bg-sky-200 flex items-center justify-center">
        <div className='grid grid-cols-2'>
          <div>
            <div className='flex w-96 h-32 bg-gray-50'>
              <h2 className='p-4 text-xl'>Mortgage Calculator</h2>
              <p className='p-4'>Clear all</p>
            </div>
            <div>
              <p>Mortgage Account</p>
              <CurrencyInput amount={amount} handleAmountChange={handleAmountChange} />
            </div>
          </div>

          <div className='w-96 h-32 bg-blue-800'>
            <h2 className='p-4 text-white text-base'>Your results</h2>
          </div>
        </div>
      </div>
    </>
  )
}

export default App
